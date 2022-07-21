package v
import chisel3.util.log2Ceil


case class DataPathParam(dataWidth: Int) {
  val dataBits: Int = log2Ceil(dataWidth)
}

/**
  * @param ELEN data width of functional unit
  * @param VLEN length of vector register
  * @param lane num of lanes
  */
case class LaneParameters(ELEN: Int = 64, VLEN: Int = 128, lane: Int = 4) {
  // lane param
  val addRespWidth: Int = ELEN + 1
  val mulRespWidth: Int = ELEN * 2
  val elenBits: Int = log2Ceil(ELEN)
  // VLEN * max LMUL / min SEW
  val VLMax: Int = VLEN
  val groupSize: Int = VLMax / lane
  val groupSizeBits: Int = log2Ceil(groupSize)
  val idBits: Int = log2Ceil(lane)

  def datePathParam: DataPathParam = DataPathParam(ELEN)
  def lanePopCountParameter: LanePopCountParameter = LanePopCountParameter(ELEN, elenBits)
  def shifterParameter: LaneShifterParameter = LaneShifterParameter(ELEN, elenBits)
  def mulParam: LaneMulParam = LaneMulParam(ELEN)
  def indexParam: LaneIndexCalculatorParameter = LaneIndexCalculatorParameter(groupSizeBits, idBits)
}
