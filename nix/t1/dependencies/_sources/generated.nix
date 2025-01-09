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
    version = "26f00d00c3f3f57480065e02bfcfde3d3b41ec51";
    src = fetchFromGitHub {
      owner = "ucb-bar";
      repo = "berkeley-hardfloat";
      rev = "26f00d00c3f3f57480065e02bfcfde3d3b41ec51";
      fetchSubmodules = false;
      sha256 = "sha256-gA1Ol7xnzC+10lGwK9+ftfJcMhKsC0KhjENQvUg3u88=";
    };
    date = "2024-06-05";
  };
  chisel = {
    pname = "chisel";
    version = "5d68beab176dcd1fb52e09aa6a07f6d01d8a9ac5";
    src = fetchFromGitHub {
      owner = "chipsalliance";
      repo = "chisel";
      rev = "5d68beab176dcd1fb52e09aa6a07f6d01d8a9ac5";
      fetchSubmodules = false;
      sha256 = "sha256-0vj9cWRZ9emxvVxfHBAN0N9hGHNucZQ0ub17b4j0Dmk=";
    };
    date = "2025-01-09";
  };
  chisel-interface = {
    pname = "chisel-interface";
    version = "57081e3e080d09bf811cce7b3936ea9e5dc187f9";
    src = fetchFromGitHub {
      owner = "chipsalliance";
      repo = "chisel-interface";
      rev = "57081e3e080d09bf811cce7b3936ea9e5dc187f9";
      fetchSubmodules = false;
      sha256 = "sha256-Pmeu5dtjPp9WMzmJY2ULWB20DFq5Vh7Y4G0/Hbjyogc=";
    };
    date = "2024-10-12";
  };
  riscv-opcodes = {
    pname = "riscv-opcodes";
    version = "07b21cc5143a15959eda12e30aa40cea0971efe0";
    src = fetchFromGitHub {
      owner = "riscv";
      repo = "riscv-opcodes";
      rev = "07b21cc5143a15959eda12e30aa40cea0971efe0";
      fetchSubmodules = false;
      sha256 = "sha256-B9njfBxZfm7xkSKBD8JOUWIKEzL8ra/X9FKC3CJ2gK8=";
    };
    date = "2024-07-24";
  };
  rvdecoderdb = {
    pname = "rvdecoderdb";
    version = "6f22826d2c8facb6bf0b41f4bea26a2225751220";
    src = fetchFromGitHub {
      owner = "sequencer";
      repo = "rvdecoderdb";
      rev = "6f22826d2c8facb6bf0b41f4bea26a2225751220";
      fetchSubmodules = false;
      sha256 = "sha256-4Hwa2Z4mmALy4ZElWzxFgqC+7EsyBhahVYlVUzyYKF4=";
    };
    date = "2024-07-25";
  };
}
