# This file was generated by nvfetcher, please do not modify it manually.
{ fetchgit, fetchurl, fetchFromGitHub, dockerTools }:
{
  arithmetic = {
    pname = "arithmetic";
    version = "4a81e23e1794844b36c53385d343475d4d7eca49";
    src = fetchFromGitHub {
      owner = "sequencer";
      repo = "arithmetic";
      rev = "4a81e23e1794844b36c53385d343475d4d7eca49";
      fetchSubmodules = false;
      sha256 = "sha256-tQwzECNOXhuKzpwRD+iKSEJYl1/wlhMQTJULJSCdTrY=";
    };
    date = "2024-01-23";
  };
  berkeley-hardfloat = {
    pname = "berkeley-hardfloat";
    version = "b3c8a38c286101973b3bc071f7918392343faba7";
    src = fetchFromGitHub {
      owner = "ucb-bar";
      repo = "berkeley-hardfloat";
      rev = "b3c8a38c286101973b3bc071f7918392343faba7";
      fetchSubmodules = false;
      sha256 = "sha256-3j6K/qFuH8PqJT6zHVTIphq9HWxmSGoIqDo9GV1bxmU=";
    };
    date = "2023-10-25";
  };
  cde = {
    pname = "cde";
    version = "2bcaeae2b9914bd25497ce3c6fa62dc5ca80e09f";
    src = fetchFromGitHub {
      owner = "chipsalliance";
      repo = "cde";
      rev = "2bcaeae2b9914bd25497ce3c6fa62dc5ca80e09f";
      fetchSubmodules = false;
      sha256 = "sha256-lhHzdXSaZ54o8P9jB7+Yjg3BWp1O7XLguBZPhAp7Hzg=";
    };
    date = "2024-04-25";
  };
  chisel = {
    pname = "chisel";
    version = "2314926b0afbdac7c5f127f111cd801197842514";
    src = fetchFromGitHub {
      owner = "chipsalliance";
      repo = "chisel";
      rev = "2314926b0afbdac7c5f127f111cd801197842514";
      fetchSubmodules = false;
      sha256 = "sha256-1G0xVzfjluzTVHG83IL7lA9EbgDhTgdp0MOu7HuURgw=";
    };
    date = "2024-06-15";
  };
  diplomacy = {
    pname = "diplomacy";
    version = "055be698f4ad55bf4a90b3d5e31d4344be5f788b";
    src = fetchFromGitHub {
      owner = "chipsalliance";
      repo = "diplomacy";
      rev = "055be698f4ad55bf4a90b3d5e31d4344be5f788b";
      fetchSubmodules = false;
      sha256 = "sha256-cfLZQ9kjUN0S4M8FoiN0AMZjMTvjgbEUWI6EyGmRLn8=";
    };
    date = "2024-04-25";
  };
  riscv-opcodes = {
    pname = "riscv-opcodes";
    version = "9fa26954e79d4403eedcbe1b35395001bbbeb8b1";
    src = fetchFromGitHub {
      owner = "riscv";
      repo = "riscv-opcodes";
      rev = "9fa26954e79d4403eedcbe1b35395001bbbeb8b1";
      fetchSubmodules = false;
      sha256 = "sha256-Gt3v8/VVNhB4IFL7kud8Y7EnSM2/2H4urV1AmBviP9E=";
    };
    date = "2024-04-10";
  };
  rocket-chip = {
    pname = "rocket-chip";
    version = "10f7d9e247d1547465b0dd70596496fbcd6a50f7";
    src = fetchFromGitHub {
      owner = "chipsalliance";
      repo = "rocket-chip";
      rev = "10f7d9e247d1547465b0dd70596496fbcd6a50f7";
      fetchSubmodules = false;
      sha256 = "sha256-ODEtLuszeb/v5ZM9v0fmUYh2jaJiHC5+vJuJMiy/X3c=";
    };
    date = "2024-03-11";
  };
  rocket-chip-inclusive-cache = {
    pname = "rocket-chip-inclusive-cache";
    version = "7f391c5e4cba3cdd4388efb778bd80da35d5574a";
    src = fetchFromGitHub {
      owner = "chipsalliance";
      repo = "rocket-chip-inclusive-cache";
      rev = "7f391c5e4cba3cdd4388efb778bd80da35d5574a";
      fetchSubmodules = false;
      sha256 = "sha256-mr3PA/wlXkC/Cu/H5T6l1xtBrK9KQQmGOfL3TMxq5T4=";
    };
    date = "2023-08-15";
  };
  rvdecoderdb = {
    pname = "rvdecoderdb";
    version = "d65525e7e18004b0877d8fbe2c435296ab986f44";
    src = fetchFromGitHub {
      owner = "sequencer";
      repo = "rvdecoderdb";
      rev = "d65525e7e18004b0877d8fbe2c435296ab986f44";
      fetchSubmodules = false;
      sha256 = "sha256-MzEoFjyUgarR62ux4ngYNFOgvAoeasdr1EVhaCvuh+Q=";
    };
    date = "2024-01-28";
  };
  tilelink = {
    pname = "tilelink";
    version = "cd177e4636eb4a20326795a66e9ab502f9b2500a";
    src = fetchFromGitHub {
      owner = "sequencer";
      repo = "tilelink";
      rev = "cd177e4636eb4a20326795a66e9ab502f9b2500a";
      fetchSubmodules = false;
      sha256 = "sha256-PIPLdZSCNKHBbho0YWGODSEM8toRBlOYC2gcbh+gqIY=";
    };
    date = "2023-08-11";
  };
}
