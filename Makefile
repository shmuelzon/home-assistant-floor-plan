VERSION=$(shell git describe --always --tags)
SRC_DIR=src/com/shmuelzon/HomeAssistantFloorPlan
SRCS=$(wildcard $(SRC_DIR)/*)
OBJS=$(subst src/,build/,$(SRCS:.java=.class))

SWEET_HOME_VERSION=7.3

SWEET_HOME_JAR=dl/SweetHome3D-$(SWEET_HOME_VERSION).jar
J3D_CORE_JAR=dl/j3dcore.jar
J3D_VECMATH_JAR=dl/vecmath.jar
JAVA_DEPENDENCIES=$(SWEET_HOME_JAR) $(J3D_CORE_JAR) $(J3D_VECMATH_JAR)
PLUGIN=HomeAssistantFloorPlanPlugin-$(VERSION).sh3p

define download
	@echo "Downloading $(notdir $1)"
	@mkdir -p dl/
	@wget -4 --quiet --show-progress -O $1 $2
endef

$(SWEET_HOME_JAR):
	$(call download,$@,https://sourceforge.net/projects/sweethome3d/files/SweetHome3D/SweetHome3D-$(SWEET_HOME_VERSION)/SweetHome3D-$(SWEET_HOME_VERSION).jar)

$(J3D_CORE_JAR):
	$(call download,$@,https://jogamp.org/deployment/java3d/1.6.0-final/j3dcore.jar)

$(J3D_VECMATH_JAR):
	$(call download,$@,https://jogamp.org/deployment/java3d/1.6.0-final/vecmath.jar)

build/%.class: src/%.java $(JAVA_DEPENDENCIES)
	javac -classpath "dl/*:build" -target 1.5 -source 1.5 -d build $<

build/%.properties: src/%.properties
	install -D $< $@

$(PLUGIN): $(OBJS)
	jar -cf $@ -C build .

build: $(PLUGIN)

clean:
	rm -rf build $(PLUGIN)

distclean: clean
	rm -rf dl

install: $(PLUGIN)
	install -D $(PLUGIN) -t ~/.eteks/sweethome3d/plugins/

test: install
	java -jar $(SWEET_HOME_JAR)

.DEFAULT_GOAL:=build
.PHONY:=build clean distclean install test
