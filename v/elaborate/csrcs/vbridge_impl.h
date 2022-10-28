#pragma once

#include <queue>
#include <optional>

#include "mmu.h"

#include "VV.h"
#include "verilated_fst_c.h"

#include "spike_event.h"
#include "simple_sim.h"
#include "rtl_event.h"
#include "vbridge_config.h"

class SpikeEvent;

struct TLBank {
  uint32_t data;
  uint16_t source;
  enum class opType {
    Nil, Get, PutFullData
  } op;
  int remainingCycles;

  TLBank();

  void step();

  [[nodiscard]] bool done() const;

  [[nodiscard]] bool ready() const;

  void clear();
};

class VBridgeImpl {
public:
  explicit VBridgeImpl();

  ~VBridgeImpl();

  void setup(const std::string &bin, const std::string &wave, uint64_t reset_vector, uint64_t cycles);

  // todo remove this.
  void configure_simulator(int argc, char **argv);

  void run();

  uint8_t load(uint64_t address);

private:
  VerilatedContext ctx;
  VV top;
  VerilatedFstC tfp;
  simple_sim sim;
  isa_parser_t isa;
  processor_t proc;
  uint64_t _cycles{};
  TLBank banks[consts::numTL];

  /// to rtl stack
  /// in the spike thread, spike should detech if this queue is full, if not full, execute until a vector instruction,
  /// record the behavior of this instruction, and send to str_stack.
  /// in the RTL thread, the RTL driver will consume from this queue, drive signal based on the queue.
  /// size of this queue should be as big as enough to make rtl free to run, reducing the context switch overhead.
  std::list<SpikeEvent> to_rtl_queue;

  const size_t to_rtl_queue_size = 10;

  /// file path of executeable binary file, which will be executed.
  std::string bin;
  /// generated waveform path.
  std::string wave;
  /// reset vector of
  uint64_t reset_vector{};
  /// RTL timeout cycles
  /// note: this is not the real system cycles, scalar instructions is evaulated via spike, which is not recorded.
  uint64_t timeout{};

  inline void reset();

  insn_fetch_t fetch_proc_insn();

  uint64_t spike_mem_load(uint64_t addr, uint32_t size);

  void spike_mem_store(uint64_t addr, uint32_t size, uint64_t data);

  std::optional<SpikeEvent> create_spike_event(insn_fetch_t fetch);

  void init_spike();

  void init_simulator();

  void terminate_simulator();

  uint64_t get_simulator_cycle();

  std::optional<SpikeEvent> spike_step();

  void update_lsu_idx();
  void loop_until_se_queue_full();
  SpikeEvent *find_se_to_issue();
  void record_rf_accesses();

  void receive_tl_req();
};
