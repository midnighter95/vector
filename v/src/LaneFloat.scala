package v

import chisel3._
import chisel3.experimental.{SerializableModule, SerializableModuleParameter}
import chisel3.util._
import hardfloat._

case class LaneFloatParam(datapathWidth: Int) extends VFUParameter with SerializableModuleParameter {
  val decodeField: BoolField = Decoder.divider //todo switch to float
  val inputBundle = new LaneFloatRequest(datapathWidth)
  val outputBundle = new LaneFloatResponse(datapathWidth)
}

/** UOP encoding
  *
  * for FMA
  * up[3] = 1 for add/sub
  * up[2] = 1 for reverse in rsub and reversed FMA
  * up[1:0] for FMA sign, 0 for positive, 1 for negative
  *
  * For DIV
  * 0001 for div
  * 0010 for rdiv
  * 1000 for sqrt
  *
  *
  * For Compare
  * 0001 EQ
  * 0000 NQ
  * 0010 LT
  * 0011 LE
  * 0100 GT
  * 0101 GE
  *
  * 1000 min
  * 1100 max
  *
  *
  * none of these 3
  *
  * 0001 SGNJ
  * 0010 SGNJN
  * 0011 SGNJX

  * 0100 classify
  * 0101 merge
  * 0110 sqrt7
  * 0111 rec7
  *
  * 1000 to float
  * 1010 to sint
  * 1001 to uint
  * 1110 to sint tr
  * 1101 to uint tr
  *
  */
class LaneFloatRequest(datapathWidth: Int) extends Bundle{
  val unsigned = Bool()
  val in0 = UInt(datapathWidth.W)
  val in1 = UInt(datapathWidth.W)
  val in2 = UInt(datapathWidth.W)
  val in2en = Bool()
  val uop = UInt(4.W)
  val FMA = Bool()
  val DIV = Bool()
  val Compare = Bool()
  val reduction = Bool()
  val roundingMode = UInt(3.W)
}

class LaneFloatResponse(datapathWidth: Int)  extends Bundle{
  val output = UInt(datapathWidth.W)
  val exceptionFlags = UInt(5.W)
}

/** construct extends LaneFloat*/

// class LaneFloat(val parameter: LaneFloatParam) extends VFUModule(parameter) with SerializableModule[LaneFloatParam]{
//   val response: LaneFloatResponse = Wire(new LaneFloatResponse(parameter.datapathWidth))
//   val request: LaneFloatRequest = connectIO(response).asTypeOf(parameter.inputBundle)
//
//   val vfpu = Module(new VFPU)
//
//
// }

class VFPU extends Module{
  val req  = IO(Flipped(Decoupled(new LaneFloatRequest(32))))
  val resp = IO(Decoupled(new LaneFloatResponse(32)))

  val recIn0 = recFNFromFN(8, 24, req.bits.in0)
  val recIn1 = recFNFromFN(8, 24, req.bits.in1)
  val recIn2 = recFNFromFN(8, 24, req.bits.in2)

  val uop = RegEnable(req.bits.uop, 0.U, req.fire)
  val OtherEn = Wire(Bool())
  OtherEn := !req.bits.FMA  && !req.bits.DIV && !req.bits.Compare

  /** DIV insn lasts muticycles
    * Not Div insn lasts 1 cycle
    */

  val DivSqrtSelect = RegEnable(req.bits.DIV    , false.B, req.fire)
  val FMASelect     = RegEnable(req.bits.FMA    , false.B, req.fire)
  val CompareSelect = RegEnable(req.bits.Compare, false.B, req.fire)
  val OtherSelect   = RegEnable(OtherEn         , false.B, req.fire)

  val DivsqrtOccupied    = RegInit(false.B)
  val FastWorking = RegInit(false.B)
  val FastValid   = RegNext(FastWorking, false.B)

  val VFPUresult = Wire(UInt(32.W))
  val VFPUflags = Wire(UInt(5.W))

  FastWorking := req.fire && !req.bits.DIV

  /** FMA
    *
    * addsub(with rsub) rsub mul  maf  rmaf
    * a  in0               in1  in0  in0  in0
    * b  1                 1    in1  in1  in2
    * c  in1               in0  0    in2  in1
    *
    * */

  /** MAF control */
  val rsub   = req.bits.FMA && uop(3,2) === 3.U
  val mul    = req.bits.FMA && (!req.bits.in2en) && (uop(3,2) === 0.U)
  val addsub = req.bits.FMA && req.bits.uop(3)
  val maf    = req.bits.FMA && req.bits.in2en && (uop(3,2) === 0.U)
  val rmaf   = req.bits.FMA && req.bits.in2en && (uop(3,2) === 1.U)

  val fmaIn0 = Mux(rsub, recIn1, recIn0)
  val fmaIn1 = Mux(addsub, 1.U, Mux(rmaf, recIn2, recIn1))
  val fmaIn2 = Mux(rsub, recIn0,
    Mux(mul, 0.U,
      Mux(maf, recIn2,
        recIn1) ))

  val mulAddRecFN = Module(new MulAddRecFN(8, 24))
  mulAddRecFN.io.op := req.bits.uop(1,0)
  mulAddRecFN.io.a := fmaIn0
  mulAddRecFN.io.b := fmaIn1
  mulAddRecFN.io.c := fmaIn2
  mulAddRecFN.io.roundingMode := req.bits.roundingMode
  mulAddRecFN.io.detectTininess := false.B

  /** DIV
    *
    * 0001 for div
    * 0010 for rdiv
    * 1000 for sqrt
    * 1001 for sqrt7
    * 1010 for rec7
    *
    *   div rdiv  sqrt      rec7   sqrt7phase0  sqrt7phase1
    * a in0  in1  dontcare  1.U    dontcare      1.U
    * b in1  in0  in0       in0    in0           result
    *
    */

  val div      = req.bits.DIV && (uop === "b0001".U)
  val rdiv     = req.bits.DIV && (uop === "b0010".U)
  val sqrt     = req.bits.DIV && (uop === "b1000".U)
  val sqrt7    = req.bits.DIV && (uop === "b1001".U)
  val rec7     = req.bits.DIV && (uop === "b1001".U)

  val sqrt7Select = RegNext(req.fire && req.bits.DIV && sqrt7 ,false.B)
  val rec7Select  = RegNext(req.fire && req.bits.DIV && rec7  ,false.B)

  val sqrt7Phase1Next = Wire(Bool())
  val sqrt7Phase1 = RegNext(sqrt7Phase1Next, false.B)
  val sqrt7Phase1Valid = RegInit(false.B)

  val divIn0 = Mux(div, recIn0,
    Mux(rdiv, recIn1,
      Mux(rec7 || sqrt7Phase1, 1.U, DontCare)))
  val divIn1 = Mux(div, recIn1,
    Mux(sqrt7Phase1, Divfnout, recIn0))

  val DivsqrtResult = Wire(UInt(32.W))
  val divSqrt = Module(new DivSqrtRecFN_small(8, 24,0))

  divSqrt.io.a := divIn0
  divSqrt.io.b := divIn1
  divSqrt.io.roundingMode := req.bits.roundingMode
  divSqrt.io.detectTininess := 0.U
  divSqrt.io.sqrtOp := uop === "b1000".U
  divSqrt.io.inValid := (req.fire && req.bits.DIV) || sqrt7Phase1Valid

  val Divfnout = fNFromRecFN(8, 24, divSqrt.io.out)
  val DivsqrtValid = Mux(sqrt7Select, divSqrt.io.outValid_sqrt && sqrt7Phase1, divSqrt.io.outValid_div || divSqrt.io.outValid_sqrt)
  DivsqrtResult := Mux(rec7Select, Divfnout(23,17),
    Mux(sqrt7Select && sqrt7Phase1, Divfnout(23,17), Divfnout))
  sqrt7Phase1Next := sqrt7Phase1Valid || (!DivsqrtValid &&  sqrt7Phase1)
  sqrt7Phase1Valid := sqrt7Select  && divSqrt.io.outValid_sqrt

  /** Compare
    *
    * 0001 EQ
    * 0000 NQ
    * 0010 LT
    * 0011 LE
    * 0100 GT
    * 0101 GE
    *
    * 1000 min
    * 1100 max
    *
    */
  val Compare = Module(new CompareRecFN(8, 24))
  Compare.io.a := recIn0
  Compare.io.b := recIn1
  Compare.io.signaling := false.B
  val CompareResult = Wire(UInt(32.W))
  val Compareflags = Wire(UInt(5.W))
  CompareResult := Mux(uop === "b0001".U, Compare.io.eq,
    Mux(uop === "b0000".U, !Compare.io.eq,
      Mux(uop === "b0010".U, Compare.io.lt,
        Mux(uop === "b0011".U, Compare.io.lt || Compare.io.eq,
          Mux(uop === "b0100".U, Compare.io.gt,
            Mux(uop === "b0101".U, Compare.io.gt || Compare.io.eq,
              Mux(uop === "b1000".U , Mux(Compare.io.lt, req.bits.in0, req.bits.in1),
                Mux(uop === "b1100".U,Mux(Compare.io.gt, req.bits.in0, req.bits.in1),
                  false.B))))))))// todo false.B for illegal
  Compareflags := Compare.io.exceptionFlags


  /** Other
    *
    * uop(3,2) =
    * 1x => convert
    * 00 => sgnj
    * 01 => clasify,merge
    *
    *
    * 0001 SGNJ
    * 0010 SGNJN
    * 0011 SGNJX
    *
    * 0100 classify
    * 0101 merge
    *
    * 1000 to float
    * 1010 to sint
    * 1001 to uint
    * 1110 to sint tr
    * 1101 to uint tr
    *
    * */
  val IntToFn = Module(new INToRecFN(32, 8, 24))
  IntToFn.io.in := req.bits.in0
  IntToFn.io.signedIn := req.bits.unsigned
  IntToFn.io.roundingMode := req.bits.roundingMode
  IntToFn.io.detectTininess := false.B

  /** io.signedOut define output sign
    *
    * false for toUInt
    * true for toInt
    */
  val FnToInt = Module(new RecFNToIN(8, 24, 32))
  val ConvertRtz = (uop(3,2) === 3.U) && req.bits.Compare
  FnToInt.io.in := recIn0
  FnToInt.io.roundingMode := Mux(ConvertRtz, "b001".U(3.W), req.bits.roundingMode)
  FnToInt.io.signedOut := !(uop(3) && uop(0))

  val ConvertResult = Wire(UInt(32.W))
  val ConvertFlags = Wire(UInt(5.W))
  ConvertResult := Mux(uop === "b1000".U, fNFromRecFN(8, 24, IntToFn.io.out), FnToInt.io.out)
  ConvertFlags  := Mux(uop === "b1000".U, IntToFn.io.exceptionFlags, FnToInt.io.intExceptionFlags)

  /** sgnj
    *
    * 0001 SGNJ
    * 0010 SGNJN
    * 0011 SGNJX
    */
  val sgnjresult = Wire(UInt(32.W))
  val sgnjSign = Mux(OtherEn && uop === 1.U, req.bits.in1(31),
    Mux(OtherEn && uop === 2.U, !req.bits.in1(31),
      Mux(OtherEn && uop ===3.U, req.bits.in1(31) ^ req.bits.in0(31), false.B)))
  sgnjresult := Cat(sgnjSign, req.bits.in0(30,0))

  val OtherResult = Wire(UInt(32.W))
  OtherResult := Mux(uop(3),ConvertResult,
    Mux(uop(3,2) === "b00".U, sgnjresult,
      Mux(uop === "b0100".U, classifyRecFN(8, 24, recFNFromFN(8, 24, req.bits.in0)), 0.U)))


  /** stateMachine */
  switch(DivsqrtOccupied) {
    is(false.B) {
      DivsqrtOccupied := req.fire && req.bits.DIV
    }
    is(true.B) {
      DivsqrtOccupied := DivsqrtValid
    }
  }

  /** Output */
  req.ready := !DivsqrtOccupied
  resp.valid := DivsqrtValid || FastValid

  VFPUresult := Mux(DivSqrtSelect, DivsqrtResult,
    Mux(FMASelect, fNFromRecFN(8, 23, mulAddRecFN.io.out),
      Mux(CompareSelect , CompareResult,
        Mux(OtherSelect , OtherResult,
          0.U))))// todo 0.U for illegal

  VFPUflags := Mux(DivSqrtSelect, divSqrt.io.exceptionFlags,
    Mux(FMASelect, fNFromRecFN(8, 23, mulAddRecFN.io.exceptionFlags),
      Mux(CompareSelect , Compareflags,
        Mux(OtherSelect && uop(3), ConvertFlags,
          0.U))))

  resp.bits.output := VFPUresult
  resp.bits.exceptionFlags := VFPUflags
}
