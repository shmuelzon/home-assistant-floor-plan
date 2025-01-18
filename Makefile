VERSION=$(shell git describe --always --tags)
export VERSION
SRC_DIR=src/com/shmuelzon/HomeAssistantFloorPlan
SRCS=$(wildcard $(SRC_DIR)/*)
OBJS=$(subst src/,build/,$(SRCS:.java=.class))

SWEET_HOME_VERSION=7.5

SWEET_HOME_JAR=dl/SweetHome3D-$(SWEET_HOME_VERSION).jar
J3D_CORE_JAR=dl/j3dcore.jar
J3D_VECMATH_JAR=dl/vecmath.jar
JAVA_DEPENDENCIES=$(SWEET_HOME_JAR) $(J3D_CORE_JAR) $(J3D_VECMATH_JAR)
PLUGIN=HomeAssistantFloorPlanPlugin-$(VERSION).sh3p

DOCKER_CMD :=
ifeq ($(wildcard /.dockerenv),)
ifneq ($(shell which docker),)
  DOCKER_CMD := docker run $(if $(TERM),-it )--rm --user $(shell id -u):$(shell id -g) --volume $(PWD):$(PWD) --workdir $(PWD) eclipse-temurin:8-noble
endif
endif

ifneq ($(V),)
  Q :=
define exec
	$3
endef
else
  Q := @
define exec
	@echo "$1\\t$2"
	@output=`$3 2>&1` || (echo "$$output"; false)
endef
endif

define download
	$(Q)mkdir -p dl/
	$(call exec,DL,$2,wget -4 --quiet --show-progress -O $1 $2)
endef

$(SWEET_HOME_JAR):
	$(call download,$@,https://sourceforge.net/projects/sweethome3d/files/SweetHome3D/SweetHome3D-$(SWEET_HOME_VERSION)/SweetHome3D-$(SWEET_HOME_VERSION).jar)

$(J3D_CORE_JAR):
	$(call download,$@,https://jogamp.org/deployment/java3d/1.6.0-final/j3dcore.jar)

$(J3D_VECMATH_JAR):
	$(call download,$@,https://jogamp.org/deployment/java3d/1.6.0-final/vecmath.jar)

build/%.class: src/%.java $(JAVA_DEPENDENCIES)
	$(call exec,JAVA,$@,$(DOCKER_CMD) javac -classpath "dl/*:src" -target 1.8 -source 1.8 -Xlint:-options -d build $<)

build/%.properties: src/%.properties
	$(Q)mkdir -p $(dir $@)
	$(call exec,GEN,$@,envsubst < $< > $@)

$(PLUGIN): $(OBJS)
	$(call exec,JAR,$@,$(DOCKER_CMD) jar -cf $@ -C build .)

build: $(PLUGIN)

clean:
	$(Q)rm -rf build *.sh3p

distclean: clean
	$(Q)rm -rf dl

install: $(PLUGIN)
	$(call exec,INSTALL,$(PLUGIN),install -D $(PLUGIN) -t ~/.eteks/sweethome3d/plugins/)

test: install
	$(Q)java -jar $(SWEET_HOME_JAR)

.DEFAULT_GOAL:=build
.PHONY:=build clean distclean install test
