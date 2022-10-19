package v.elaborate

import chisel3.aop.Select
import chisel3.aop.injecting.InjectingAspect
import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.{CIRCTHandover, CIRCTTarget, CIRCTTargetAnnotation, ChiselStage, FirtoolOption}
import firrtl.{AnnotationSeq, ChirrtlEmitter, EmitAllModulesAnnotation}
import firrtl.options.TargetDirAnnotation
import logger.{LogLevel, LogLevelAnnotation}
import mainargs._
import v.{LSU, V, VRF}

object Main {
  @main def elaborate(@arg(name="dir") dir: String) = {
    val annotations = Seq(new ChiselStage).foldLeft(
      Seq(
        TargetDirAnnotation(dir),
        EmitAllModulesAnnotation(classOf[ChirrtlEmitter]),
        CIRCTTargetAnnotation(CIRCTTarget.Verilog),
        FirtoolOption(s"""-O=debug"""),
        CIRCTHandover(CIRCTHandover.CHIRRTL),
        ChiselGeneratorAnnotation(() => new v.V(v.VParam())),
        InjectingAspect(
          { dut: V =>
            Select.collectDeep(dut) {
              case vrf: VRF => vrf
            }
          },
          { vrf: VRF =>chisel3.experimental.Trace.traceName(vrf.write.valid) }
        ),
        InjectingAspect(
          { dut: V =>
            Select.collectDeep(dut) {
              case lsu: LSU => lsu
            }
          },
          { lsu: LSU => chisel3.experimental.Trace.traceName(lsu.reqEnq) }
        ),
      ): AnnotationSeq
    ) { case (annos, stage) => stage.transform(annos) }
  }
  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)
}