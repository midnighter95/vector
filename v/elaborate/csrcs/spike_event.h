#pragma once

#include <queue>
#include <optional>

#include "mmu.h"

#include "VV.h"
#include "verilated_fst_c.h"

#include "simple_sim.h"

struct SpikeEvent {
  explicit SpikeEvent(processor_t &proc, insn_fetch_t &fetch);

  [[nodiscard]] std::string get_insn_disasm() const;

  void drive_rtl_req(VV &top) const;
  void drive_rtl_csr(VV &top) const;

  void log();

  commit_log_mem_t mem_read_info;

  uint64_t mem_load(uint64_t addr, uint32_t size);

  struct mem_log {
    uint64_t addr;
    uint64_t value;
    uint8_t size;
  };
  std::vector<mem_log> log_mem_queue;

  uint8_t lsu_idx = 255;
  processor_t &proc;

  bool is_issued;
  bool is_committed;

  bool is_load;
  bool is_store;

  uint64_t pc;
  uint32_t inst_bits;

  // scalar to vector interface(used for driver)
  uint32_t rs1_bits;
  uint32_t rs2_bits;

  // vtype
  uint32_t vsew: 3;
  uint32_t vlmul: 3;
  bool vma: 1;
  bool vta: 1;
  uint32_t vxrm: 2;

  // other CSR
  bool vill: 1;
  bool vxsat: 1;

  /// range [XLEN-1:0].
  /// updated with vset{i}vl{i} and fault-only-first vector load instruction variants
  /// currently, we don't implement MMU, thus, no fault-only-first will be executed.
  uint32_t vl;
  uint16_t vstart;

  /// pipeline control signal with core
  bool _ignore_exception = false;  // TODO: give it correct value
  bool _store_buffer_clear = false;  // TODO: give it correct value
};
