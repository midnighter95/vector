// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2022 Jiuyang Liu <liu@jiuyang.me>

package org.chipsalliance.t1.script

import mainargs.{arg, main, Flag, Leftover, ParserForMethods, TokensReader}
import scala.io.AnsiColor._

object Logger {
  val level = sys.env.getOrElse("LOG_LEVEL", "INFO") match
    case "TRACE" | "trace" => 0
    case "ERROR" | "error" => 1
    case "INFO" | "info"   => 2
    case _                 => 4

  def info(message: String) =
    if level <= 2 then println(s"${BOLD}${GREEN}[INFO]${RESET} ${message}")

  def trace(message: String) =
    if level <= 0 then println(s"${BOLD}${GREEN}[TRACE]${RESET} ${message}")

  def error(message: String) =
    if level <= 2 then println(s"${BOLD}${RED}[ERROR]${RESET} ${message}")

  def fatal(message: String) =
    println(s"${BOLD}${RED}[FATAL]${RESET} ${message}")
    sys.exit(1)
}

object Main:
  implicit object PathRead extends TokensReader.Simple[os.Path]:
    def shortName = "path"

    def read(strs: Seq[String]) = Right(os.Path(strs.head, os.pwd))

  def resolveNixPath(attr: String, extraArgs: Seq[String] = Seq()): String =
    Logger.trace(s"Running nix build ${attr}")
    val args = Seq(
      "nix",
      "build",
      "--no-link",
      "--no-warn-dirty",
      "--print-out-paths",
      attr
    ) ++ extraArgs
    os.proc(args).call().out.trim()

  def resolveTestElfPath(
    config:   String,
    caseName: String,
    forceX86: Boolean = false
  ): os.Path =
    val casePath = os.Path(caseName, os.pwd)
    if (os.exists(casePath)) then return casePath

    val caseAttrRoot =
      if (forceX86) then ".#legacyPackages.x86_64-linux."
      else ".#"

    val nixStorePath = resolveNixPath(
      s"${caseAttrRoot}t1.${config}.ip.cases.${caseName}"
    )
    val elfFilePath  = os.Path(nixStorePath) / "bin" / s"${caseName}.elf"

    elfFilePath
  end resolveTestElfPath

  def resolveTestBenchPath(
    config:  String,
    emuType: String
  ): os.Path =
    val emuPath = os.Path(emuType, os.pwd)
    if (os.exists(emuPath)) then return emuPath

    val nixStorePath =
      if emuType.contains("vcs-") then resolveNixPath(s".#t1.${config}.ip.${emuType}", Seq("--impure"))
      else resolveNixPath(s".#t1.${config}.ip.${emuType}")

    val elfFilePath = os
      .walk(os.Path(nixStorePath) / "bin")
      .filter(path => os.isFile(path))
      .lastOption
      .getOrElse(Logger.fatal("no simulator found in given attribute"))

    elfFilePath
  end resolveTestBenchPath

  def prepareOutputDir(
    outputDir: String
  ): os.Path =
    val outputPath  = os.Path(outputDir, os.pwd)
    val currentDate = java.time.LocalDateTime
      .now()
      .format(
        java.time.format.DateTimeFormatter.ofPattern("yy-MM-dd-HH-mm-ss")
      )
    val resultPath  = outputPath / "all" / currentDate

    os.makeDir.all(resultPath)

    val userPath = outputPath / "result"
    os.remove(userPath, checkExists = false)
    os.symlink(userPath, resultPath)

    userPath
  end prepareOutputDir

  // naive case validation
  def isValidCaseName(caseName: String): Boolean =
    caseName.split("\\.").toSeq.length == 2

  def tryRestoreFromCache(key: String, value: Option[String]): Option[String] =
    val xdgDir  = sys.env.get("XDG_CACHE_HOME")
    val homeDir = sys.env.get("HOME")

    val cacheDir =
      if xdgDir != None then os.Path(xdgDir.get) / "chipsalliance-t1"
      else if homeDir != None then os.Path(homeDir.get) / ".cache" / "chipsalliance-t1"
      else os.pwd / ".cache"

    os.makeDir.all(cacheDir)

    val cacheFile = cacheDir / "helper-cache.json"
    val cache     =
      if os.exists(cacheFile) then ujson.read(os.read(cacheFile))
      else ujson.Obj()

    val ret = if value.isDefined then
      cache(key) = value.get
      value
    else cache.obj.get(key).map(v => v.str)

    os.write.over(cacheFile, ujson.write(cache))

    ret
  end tryRestoreFromCache

  def optionals(cond: Boolean, input: Seq[String]): Seq[String] =
    if (cond) then input else Seq()

  def optionalMap[K, V](cond: Boolean, input: Map[K, V]): Map[K, V] =
    if (cond) then input else null

  @main def run(
    @arg(
      name = "emu",
      short = 'e',
      doc = "Type for emulator, Eg. vcs-emu, verilator-emu-trace"
    ) emuType:  Option[String],
    @arg(
      name = "config",
      short = 'c',
      doc = "configuration name"
    ) config:   Option[String],
    @arg(
      name = "verbose",
      short = 'v',
      doc = "set loglevel to debug"
    ) verbose:  Flag = Flag(false),
    @arg(
      name = "out-dir",
      doc = "path to save wave file and perf result file"
    ) outDir:   Option[String] = None,
    @arg(
      doc = "Cross compile RISC-V test case with x86-64 host tools"
    ) forceX86: Boolean = false,
    @arg(
      name = "dry-run",
      doc = "Print the final emulator command line and exit"
    ) dryRun:   Flag = Flag(false),
    leftOver:   Leftover[String]
  ): Unit =
    if leftOver.value.isEmpty then Logger.fatal("No test case name")
    val caseName = leftOver.value.head
    if !isValidCaseName(caseName) then Logger.fatal(s"invalid caseName '$caseName', expect 'A.B'")

    val finalEmuType = tryRestoreFromCache("emulator", emuType)
    if finalEmuType.isEmpty then
      Logger.fatal(
        s"No cached emulator selection nor --emu argument was provided"
      )

    val isTrace = finalEmuType.get.endsWith("-trace")

    val finalConfig = tryRestoreFromCache("config", config)
    if finalConfig.isEmpty then
      Logger.fatal(
        s"No cached config selection nor --config argument was provided"
      )

    Logger.info(
      s"Using config=${BOLD}${finalConfig.get}${RESET} emulator=${BOLD}${finalEmuType.get}${RESET} case=${BOLD}$caseName${RESET}"
    )

    val caseElfPath =
      resolveTestElfPath(finalConfig.get, caseName, forceX86)
    val outputPath  = prepareOutputDir(outDir.getOrElse("t1-sim-result"))
    val emulator    = resolveTestBenchPath(finalConfig.get, finalEmuType.get)

    val leftOverArguments = leftOver.value.dropWhile(arg => arg != "--")

    val processArgs = Seq(
      emulator.toString(),
      s"+t1_elf_file=${caseElfPath}"
    )
      ++ optionals(isTrace, Seq(s"+t1_wave_path=${outputPath / "wave.fsdb"}"))
      ++ optionals(!leftOverArguments.isEmpty, leftOverArguments)

    if dryRun.value then return

    val rtlEventPath = outputPath / "rtl-event.jsonl.zst"
    val journalPath  = outputPath / "online-drive-emu-journal"
    val statePath    = outputPath / "driver-state.json"

    // Save information of this run, so that user can start offline check without arguments
    os.write(
      statePath,
      ujson.write(
        ujson.Obj(
          "config" -> finalConfig.get,
          "elf"    -> caseElfPath.toString,
          "event"  -> rtlEventPath.toString
        )
      )
    )

    // For vcs trace simulator, we need daidir keep at same directory as the wave.fsdb file
    if finalEmuType.get == "vcs-emu-trace" then
      val libPath    = emulator / os.up / os.up / "lib"
      val daidirPath =
        os.walk(libPath)
          .filter(path => os.isDir(path))
          .filter(path => path.segments.toSeq.last.endsWith(".daidir"))
          .last
      val daidirName = daidirPath.segments.toSeq.last
      os.symlink(outputPath / daidirName, daidirPath)

    Logger.info(s"Starting IP emulator: `${processArgs.mkString(" ")}`")

    val driverProc = os
      .proc(processArgs)
      .spawn(
        stdout = journalPath,
        stderr = os.Pipe,
        env = optionalMap(verbose.value, Map("RUST_LOG" -> "TRACE"))
      )
    val zstdProc   = os
      .proc(Seq("zstd", "-o", s"${rtlEventPath}"))
      .spawn(
        stdin = driverProc.stderr,
        stdout = os.Inherit,
        stderr = os.Inherit
      )

    zstdProc.join(-1)
    driverProc.join(-1)
    if zstdProc.exitCode() != 0 then Logger.fatal("fail to compress data")
    if driverProc.exitCode() != 0 then Logger.fatal("online driver run failed")

    Logger.info("Driver finished")

    if os.exists(os.pwd / "perf.json") then
      os.move(os.pwd / "perf.json", outputPath / "perf.json", replaceExisting = true)

    Logger.info(s"Output saved under ${outputPath}")
  end run

  @main
  def offline(
    @arg(
      name = "config",
      short = 'c',
      doc = "specify the elaborate config for running test case"
    ) config:    Option[String],
    @arg(
      name = "case-attr",
      short = 'C',
      doc = "Specify test case attribute to run diff test"
    ) caseAttr:  Option[String],
    @arg(
      name = "event-path",
      short = 'e',
      doc = "Specify the event log path to examinate"
    ) eventPath: Option[String],
    @arg(
      name = "verbose",
      short = 'v',
      doc = "Verbose output"
    ) verbose:   Flag = Flag(false),
    @arg(
      name = "out-dir",
      doc = "path to save wave file and perf result file"
    ) outDir:    Option[String] = None
  ): Unit =
    val logLevel =
      if verbose.value then "trace"
      else "info"

    val resultPath =
      os.Path(outDir.getOrElse("t1-sim-result"), os.pwd) / "result"
    val lastState  =
      if os.exists(resultPath) then ujson.read(os.read(resultPath / "driver-state.json"))
      else ujson.Obj()

    val finalConfig =
      if config.isDefined then config.get
      else
        lastState.obj
          .get("config")
          .getOrElse(Logger.fatal("No driver-state.json nor --config"))
          .str

    val offlineChecker = os.Path(
      resolveNixPath(s".#t1.${finalConfig}.ip.offline-checker")
    ) / "bin" / "offline"

    val elfFile =
      if caseAttr.isDefined then resolveTestElfPath(finalConfig, caseAttr.get).toString
      else
        lastState.obj
          .get("elf")
          .getOrElse(Logger.fatal("No driver-state.json nor --case-attr"))
          .str

    val eventFile    =
      if eventPath.isDefined then os.Path(eventPath.get, os.pwd)
      else os.Path(lastState.obj.get("event").getOrElse(Logger.fatal("")).str)
    val decEventFile = resultPath / "rtl-event.jsonl"
    Logger.info(s"Decompressing ${eventFile}")
    os.proc("zstd", s"${eventFile}", "-d", "-f", "-o", s"${decEventFile}")
      .call()

    val driverArgs: Seq[String] =
      Seq(
        offlineChecker.toString,
        "--elf-file",
        elfFile,
        "--log-level",
        logLevel,
        "--log-file",
        decEventFile.toString
      )
    Logger.info(s"Running offline checker: ${driverArgs.mkString(" ")}")

    val ret = os.proc(driverArgs).call(stdout = os.Inherit, stderr = os.Inherit, check = false)
    if (ret.exitCode != 0) then Logger.fatal("offline checker run failed")
  end offline

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)
end Main
