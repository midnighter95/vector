// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2022 Jiuyang Liu <liu@jiuyang.me>

package tests.elaborate

import chisel3._
import chisel3.experimental.SerializableModuleGenerator
import v.{V, VParameter}
import chisel3.probe._
import chisel3.util.experimental.BoringUtils.bore
import chisel3.util.DecoupledIO
import tilelink.TLChannelD


class TestBench(generator: SerializableModuleGenerator[V, VParameter]) extends RawModule {
  val clock = Wire(Clock())
  val reset = Wire(Bool())
  val dut = withClockAndReset(clock, reset)(Module(generator.module()))
  withClockAndReset(clock, reset) {
//    val coverModule = Module(new CoverModule(dut))
    val monitor = Module(new Monitor(dut))
    monitor.clock := clock
    monitor.reset := reset
  }

  val verificationModule = Module(new VerificationModule(dut))
  dut.request <> verificationModule.req
  dut.response <> verificationModule.resp
  dut.csrInterface <> verificationModule.csrInterface
  dut.storeBufferClear <> verificationModule.storeBufferClear
  dut.memoryPorts <> verificationModule.tlPort
  clock := verificationModule.clock
  reset := verificationModule.reset

  /**
    * [[v.LoadUnit]] related probe connection
    */
  val loadUnitMonitor = Module(new LoadUnitMonitor)
  loadUnitMonitor.clock.ref := clock.asBool
  loadUnitMonitor.statusIdle.ref := read(bore(dut.lsu.loadUnit.statusProbe.idle))
  loadUnitMonitor.statusLast.ref := read(bore(dut.lsu.loadUnit.statusProbe.last))
  loadUnitMonitor.tlPortAIsValid.ref := read(bore(dut.lsu.loadUnit.tlPortAProbe.valid))
  loadUnitMonitor.tlPortAIsReady.ref := read(bore(dut.lsu.loadUnit.tlPortAProbe.ready))
  loadUnitMonitor.writeReadyForLSU.ref := read(bore(dut.lsu.loadUnit.writeReadyForLSUProbe))

  read(bore(dut.lsu.loadUnit.tlPortDProbe)).zipWithIndex.foreach { case(port, i) =>
    val loadUnitPortDMonitor = Module(new LoadUnitPortDMonitor)
    loadUnitPortDMonitor.clock.ref := clock.asBool
    loadUnitPortDMonitor.portDIndex.ref := i.U
    loadUnitPortDMonitor.portDIsReady.ref := port.ready
    loadUnitPortDMonitor.portDIsValid.ref := port.valid
  };

  read(bore(dut.lsu.loadUnit.vrfWritePortProbe)).zipWithIndex.foreach({ case(port, i) =>
    val vrfWritePortMonitor = Module(new LoadUnitVrfWritePortMonitor)
    vrfWritePortMonitor.clock.ref := clock.asBool
    vrfWritePortMonitor.index.ref := i.U
    vrfWritePortMonitor.isReady.ref := port.ready
    vrfWritePortMonitor.isValid.ref := port.valid
  })

  read(bore(dut.lsu.loadUnit.lastCacheLineAckProbe)).zipWithIndex.foreach({ case(stat, i) =>
    val cacheLineAckMonitor = Module(new LoadUnitLastCacheLineAckMonitor)
    cacheLineAckMonitor.clock.ref := clock.asBool
    cacheLineAckMonitor.index.ref := i.U
    cacheLineAckMonitor.isAck.ref := stat
  })

  read(bore(dut.lsu.loadUnit.cacheLineDequeueProbe)).zipWithIndex.foreach({ case(cacheLine, i) =>
    val cacheLineDequeueMonitor = Module(new LoadUnitCacheLineDequeueMonitor)
    cacheLineDequeueMonitor.clock.ref := clock.asBool
    cacheLineDequeueMonitor.index.ref := i.U
    cacheLineDequeueMonitor.isReady.ref := cacheLine.ready
    cacheLineDequeueMonitor.isValid.ref := cacheLine.valid
  })
  // End of [[v.LoadUnit]] probe connection


  /**
    * [[v.SimpleAccessUnit]] related probe connection
    */
  val simpleAccessUnitMonitor = Module(new SimpleAccessUnitMonitor)
  simpleAccessUnitMonitor.clock.ref := clock.asBool
  simpleAccessUnitMonitor.lsuRequestIsValid.ref := read(bore(dut.lsu.otherUnit.lsuRequestProbe.valid))
  simpleAccessUnitMonitor.vrfReadDataPortsIsReady.ref := read(bore(dut.lsu.otherUnit.vrfReadDataPortsProbe.ready))
  simpleAccessUnitMonitor.vrfReadDataPortsIsValid.ref := read(bore(dut.lsu.otherUnit.vrfReadDataPortsProbe.valid))
  simpleAccessUnitMonitor.maskSelectIsValid.ref := read(bore(dut.lsu.otherUnit.maskSelectValidProbe))
  simpleAccessUnitMonitor.vrfWritePortIsReady.ref := read(bore(dut.lsu.otherUnit.vrfWritePortIsReadyProbe))
  simpleAccessUnitMonitor.vrfWritePortIsValid.ref := read(bore(dut.lsu.otherUnit.vrfWritePortIsValidProbe))
  simpleAccessUnitMonitor.currentLane.ref := read(bore(dut.lsu.otherUnit.currentLaneProbe))
  simpleAccessUnitMonitor.statusIsOffsetGroupEnd.ref := read(bore(dut.lsu.otherUnit.statusIsOffsetGroupEndProbe))
  simpleAccessUnitMonitor.statusIsWaitingFirstResponse.ref := read(bore(dut.lsu.otherUnit.statusIsWaitingFirstResponseProbe))
  simpleAccessUnitMonitor.s0Fire.ref := read(bore(dut.lsu.otherUnit.s0FireProbe))
  simpleAccessUnitMonitor.s1Fire.ref := read(bore(dut.lsu.otherUnit.s1FireProbe))
  simpleAccessUnitMonitor.s2Fire.ref := read(bore(dut.lsu.otherUnit.s2FireProbe))

  dut.lsu.otherUnit.offsetReadResultValidProbe.zipWithIndex.foreach({ case(probe, i) =>
    val monitor = Module(new SimpleAccessUnitOffsetReadResultMonitor)
    monitor.clock.ref := clock.asBool
    monitor.index.ref := i.U
    monitor.offsetReadResultIsValid.ref := read(bore(probe))
  })

  dut.lsu.otherUnit.indexedInsturctionOffsetsIsValidProbe.zipWithIndex.foreach({ case(probe, i) =>
    val monitor = Module(new SimpleAccessUnitIndexedInsnOffsetsIsValidMonitor)
    monitor.clock.ref := clock.asBool
    monitor.index.ref := i.U
    monitor.isValid.ref := read(bore(probe))
  })
  // End of [[v.SimpleAccessUnit]] related probe connection

  /**
   * [[v.StoreUnit]] related probe connection
   */
  val storeUnitMonitor = Module(new StoreUnitMonitor)
  storeUnitMonitor.clock.ref := clock.asBool
  storeUnitMonitor.vrfReadyToStore.ref := read(bore(dut.lsu.storeUnit.vrfReadyToStoreProbe))
  storeUnitMonitor.alignedDequeueReady.ref := read(bore(dut.lsu.storeUnit.alignedDequeueReadyProbe))
  storeUnitMonitor.alignedDequeueValid.ref := read(bore(dut.lsu.storeUnit.alignedDequeueValidProbe))

  dut.lsu.storeUnit.tlPortAIsReadyProbe.zipWithIndex.foreach({ case(probe, i) =>
    val monitor = Module(new StoreUnitTlPortAReadyMonitor)
    monitor.clock.ref := clock.asBool
    monitor.index.ref := i.U
    monitor.ready.ref := read(bore(probe))
  })

  dut.lsu.storeUnit.tlPortAIsValidProbe.zipWithIndex.foreach({ case(probe, i) =>
    val monitor = Module(new StoreUnitTlPortAValidMonitor)
    monitor.clock.ref := clock.asBool
    monitor.index.ref := i.U
    monitor.valid.ref := read(bore(probe))
  })

  dut.lsu.storeUnit.vrfReadDataPortIsReadyProbe.zipWithIndex.foreach({ case(probe, i) =>
    val monitor = Module(new StoreUnitVrfReadDataPortReadyMonitor)
    monitor.clock.ref := clock.asBool
    monitor.index.ref := i.U
    monitor.ready.ref := read(bore(probe))
  })

  dut.lsu.storeUnit.vrfReadDataPortIsValidProbe.zipWithIndex.foreach({ case(probe, i) =>
    val monitor = Module(new StoreUnitVrfReadDataPortValidMonitor)
    monitor.clock.ref := clock.asBool
    monitor.index.ref := i.U
    monitor.valid.ref := read(bore(probe))
  })

  dut.laneVec.zipWithIndex.foreach({ case (lane, i) =>
    {
      val monitor = Module(new LaneReadBusPortMonitor)
      monitor.clock.ref := clock.asBool
      monitor.laneIndex.ref := i.U

      monitor.readBusPortEnqReady.ref := read(bore(lane.readBusPortEnqReadyProbe))
      monitor.readBusPortEnqValid.ref := read(bore(lane.readBusPortEnqValidProbe))
      monitor.readBusPortDeqReady.ref := read(bore(lane.readBusPortDeqReadyProbe))
      monitor.readBusPortDeqValid.ref := read(bore(lane.readBusPortDeqValidProbe))
    }
    {
      val monitor = Module(new LaneWriteBusPortMonitor)
      monitor.clock.ref := clock.asBool
      monitor.laneIndex.ref := i.U

      monitor.writeBusPortDeqReady.ref := read(bore(lane.writeBusPortDeqReadyProbe))
      monitor.writeBusPortDeqValid.ref := read(bore(lane.writeBusPortDeqValidProbe))
      monitor.writeBusPortEnqReady.ref := read(bore(lane.writeBusPortEnqReadyProbe))
      monitor.writeBusPortEnqValid.ref := read(bore(lane.writeBusPortEnqValidProbe))
    }
    {
      val monitor = Module(new LaneRequestMonitor)
      monitor.clock.ref := clock.asBool
      monitor.laneIndex.ref := i.U

      monitor.laneRequestReady.ref := read(bore(lane.laneRequestReadyProbe))
      monitor.laneRequestValid.ref := read(bore(lane.laneRequestValidProbe))
    }
    {
      val monitor = Module(new LaneResponseMonitor)
      monitor.clock.ref := clock.asBool
      monitor.laneIndex.ref := i.U

      monitor.laneResponseValid.ref := read(bore(lane.laneResponseValidProbe))
      monitor.laneResponseFeedbackValid.ref := read(bore(lane.laneResponseFeedbackValidProbe))
    }
    {
      val monitor = Module(new LaneVrfReadMonitor)
      monitor.clock.ref := clock.asBool
      monitor.laneIndex.ref := i.U

      monitor.vrfReadAddressChannelReady.ref := read(bore(lane.vrfReadAddressChannelReadyProbe))
      monitor.vrfReadAddressChannelValid.ref := read(bore(lane.vrfReadAddressChannelValidProbe))
    }
    {
      val monitor = Module(new LaneVrfWriteMonitor)
      monitor.clock.ref := clock.asBool
      monitor.laneIndex.ref := i.U

      monitor.vrfWriteChannelValid.ref := read(bore(lane.vrfWriteChannelValidProbe))
      monitor.vrfWriteChannelReady.ref := read(bore(lane.vrfWriteChannelReadyProbe))
    }
    {
      val monitor = Module(new LaneStatusMonitor)
      monitor.clock.ref := clock.asBool
      monitor.laneIndex.ref := i.U

      monitor.v0UpdateValid.ref := read(bore(lane.v0UpdateValidProbe))
      monitor.writeReadyForLsu.ref := read(bore(lane.writeReadyForLsuProbe))
      monitor.vrfReadyToStore.ref := read(bore(lane.vrfReadyToStoreProbe))
    }
    {
      val monitor = Module(new LaneWriteQueueMonitor)
      monitor.clock.ref := clock.asBool
      monitor.laneIndex.ref := i.U

      monitor.writeQueueValid.ref := read(bore(lane.writeQueueValidProbe))
    }
    {
      val monitor = Module(new LaneReadBusDequeueMonitor)
      monitor.clock.ref := clock.asBool
      monitor.laneIndex.ref := i.U

      monitor.readBusDequeueValid.ref := read(bore(lane.readBusDequeueValidProbe))
    }
    {
      val monitor = Module(new CrossLaneMonitor)
      monitor.clock.ref := clock.asBool
      monitor.laneIndex.ref := i.U

      monitor.crossLaneReadValid.ref := read(bore(lane.crossLaneReadValidProbe))
      monitor.crossLaneWriteValid.ref := read(bore(lane.crossLaneWriteValidProbe))
    }
    {
      val monitor = Module(new CrossLaneMonitor)
      monitor.clock.ref := clock.asBool
      monitor.laneIndex.ref := i.U

      monitor.crossLaneReadValid.ref := read(bore(lane.crossLaneReadValidProbe))
      monitor.crossLaneWriteValid.ref := read(bore(lane.crossLaneWriteValidProbe))
    }
    {
      val monitor = Module(new LaneReadBusDataMonitor)
      monitor.clock.ref := clock.asBool
      monitor.laneIndex.ref := i.U

      monitor.readBusDataReqValid.ref := read(bore(lane.readBusDataReqValidProbe))
    }
    {
      val monitor = Module(new LaneWriteBusDataMonitor)
      monitor.clock.ref := clock.asBool
      monitor.laneIndex.ref := i.U

      monitor.writeBusDataReqValid.ref := read(bore(lane.writeBusDataReqValidProbe))
    }
  })

}
