{ pkgs ? import <nixpkgs> {}}:

let
  myLLVM = pkgs.llvmPackages_14;
  espresso = pkgs.stdenv.mkDerivation rec {
    pname = "espresso";
    version = "2.4";
    nativeBuildInputs = [ pkgs.cmake ];
    src = pkgs.fetchFromGitHub {
      owner = "chipsalliance";
      repo = "espresso";
      rev = "v${version}";
      sha256 = "sha256-z5By57VbmIt4sgRgvECnLbZklnDDWUA6fyvWVyXUzsI=";
    };
  };
  circt = pkgs.stdenv.mkDerivation rec {
    pname = "circt";
    version = "r4396.ce85204ca";
    nativeBuildInputs = with pkgs; [ cmake ninja python3 git ];
    src = pkgs.fetchFromGitHub {
      owner = "llvm";
      repo = "circt";
      rev = "da364ea448b70aaa710f9e17063ed745db23b463";
      sha256 = "sha256-oS+wh/hJeJcXj5RoxsUOOX8wMsxgMAMdQtPpyENO0JM=";
      fetchSubmodules = true;
    };
    cmakeFlags = [
      "-S/build/source/llvm/llvm"
      "-DLLVM_ENABLE_PROJECTS=mlir"
      "-DBUILD_SHARED_LIBS=OFF"
      "-DLLVM_STATIC_LINK_CXX_STDLIB=ON"
      "-DLLVM_ENABLE_ASSERTIONS=ON"
      "-DLLVM_BUILD_EXAMPLES=OFF"
      "-DLLVM_ENABLE_BINDINGS=OFF"
      "-DLLVM_ENABLE_OCAMLDOC=OFF"
      "-DLLVM_OPTIMIZED_TABLEGEN=ON"
      "-DLLVM_EXTERNAL_PROJECTS=circt"
      "-DLLVM_EXTERNAL_CIRCT_SOURCE_DIR=/build/source"
      "-DLLVM_BUILD_TOOLS=ON"
    ];
    installPhase = ''
      mkdir -p $out/bin
      mv bin/firtool $out/bin/firtool
    '';
  };

in pkgs.mkShellNoCC {
    name = "vector";
    buildInputs = with pkgs; [
      myLLVM.clang
      myLLVM.llvm
      myLLVM.bintools

      jdk mill python3
      parallel protobuf ninja verilator antlr4 numactl dtc glibc_multi cmake
      espresso
      circt

      git cacert # make cmake fetchContent happy
      fmt glog
    ];
    shellHook = ''
      export NIX_CC=" "
    '';  # due to some cmake bug
  }
