# Contributing Guidelines

All types of contributions are encouraged and valued. Please make sure to read
relevant sections in this file and in README before making your contribution. It
will make it a lot easier for us maintainers and smooth out the experience for
all involved. This project looks forward to your contributions :tada:

## How to contribute

### On \*nix Systems

* Run `make` to build the plugin.
* Run `make test` to install the plugin and start SH3D
* Run `make clean` to remove the build directory and any `.sh3p` files
* Run `make distclean` to perform `make clean` and also removes the `dl`
  directory
* Run `make install` to install the plugin to the specified directory
  (`~/.eteks/sweethome3d/plugins/`)

### On Windows Systems

Please use Windows Subsystem for Linux (WSL) to run the commands above

## Submitting  Pull Request

* Ensure any install or build dependencies are working and the code is
  generating the right output before submitting a PR
* Update the README.md with details of changes to the interface, this includes
  new fields, features, etc.
* Promptly address any CI failures. If your pull request fails to build or pass
  tests, please push another commit to fix it
* Resolve any merge conflicts that occur
