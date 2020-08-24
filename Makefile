# This is the main dispatch Makefile which controls all aspects of the build.

# === Early setup ==============

# Default goal for "make"
.DEFAULT_GOAL = ledger

# Shell for target commands
SHELL = /bin/bash

# Disable built-in rules
MAKEFLAGS += --no-builtin-rules
.SUFFIXES:

## Path to build directory
BUILD ?= build

## Clean the build directory
clean: unapply-patches
	@rm -rf $(BUILD)
	@rm -rf ledger/build
	@rm -rf ledger/.cxx

# === Autogenerated help =======

## Read this help
help:
	@echo "Targets:"
	@awk 'BEGIN { FS = ":.*?" } \
	      /^## / { split($$0, v, /## /); comment = v[2] } \
	      /^[a-zA-Z_-]+:.*?/ { \
	          if (length(comment) == 0) { next }; \
	          printf("%s\t%s\n", $$1, comment); \
	          comment = ""; \
        }' \
	 $(MAKEFILE_LIST) | sort | column -t -s $$'\t' | sed 's/^/    /'
	@echo
	@echo "Environment variables:"
	@awk 'BEGIN { FS = " *\\?= *" } \
	      /^## / { split($$0, v, /## /); comment = v[2] } \
	      /^[a-zA-Z_-]+ *\?=.*/ { \
	          if (length(comment) == 0) { next }; \
	          printf("%s\t%s\t(default: %s)\n", $$1, comment, $$2); \
	          comment = ""; \
        }' \
	 $(MAKEFILE_LIST) | sort | column -t -s $$'\t' | sed 's/^/    /'

# === Git helpers ==============

# We're using submodules and it is easy to forget to check them out after
# cloning the repository. Initialize the submodules if they are missing.
check-submodules: $(BUILD)/.check-submodules

$(BUILD)/.check-submodules:
	@git submodule update --init
	@mkdir -p $(@D)
	@touch $@

# === Docker ===================

## Docker image to use
DOCKER_IMAGE ?= ilammy/android-ledger-cli:latest

## Force Docker image rebuild
DOCKER_FORCE_BUILD ?= no

## Use Docker build cache
DOCKER_CACHE ?= yes

ifeq ($(DOCKER_CACHE),no)
DOCKER_BUILD_OPTS += --no-cache
endif

## Build Docker image
docker-image: $(BUILD)/.docker-image

# If the build is forced, ignore the stamp file presence.
ifeq ($(DOCKER_FORCE_BUILD),yes)
.PHONY: $(BUILD)/.docker-image
endif

# First, try pulling the latest version of the image. That might fail.
# Check if the image is available, and if not then build and tag one.
# Then check it again, now for real. If it works, create a stamp file.
$(BUILD)/.docker-image: $(BUILD)/.check-submodules
	@echo "Checking $(DOCKER_IMAGE)..."
	@docker image pull $(DOCKER_IMAGE) 2>/dev/null || true
	@(test "$(DOCKER_FORCE_BUILD)" = "no" && docker run $(DOCKER_IMAGE) true) || \
	 (docker build --tag=$(DOCKER_IMAGE) $(DOCKER_BUILD_OPTS) docker && \
	  docker/scripts/prefetch-gradle.sh $(DOCKER_IMAGE))
	@docker run $(DOCKER_IMAGE) true
	@echo "Docker image $(DOCKER_IMAGE) ready"
	@mkdir -p $(@D)
	@touch $@

# === Ledger ===================

# Path to resulting AAR
AAR_PATH = ledger/build/outputs/aar/ledger-release.aar

# Path to build directory inside Docker container
DOCKER_PATH = /home/user/android-ledger-cli

## Build Ledger AAR
ledger: $(AAR_PATH)

$(AAR_PATH): $(BUILD)/.check-submodules $(BUILD)/.docker-image $(BUILD)/.patch-ledger
	@echo "Building Ledger..."
	@docker run --rm -v $(PWD):$(DOCKER_PATH) \
	     $(DOCKER_IMAGE) \
	     /bin/bash -c "cd $(DOCKER_PATH) && ./gradlew --no-daemon assembleRelease"
	@echo
	@echo "Output AAR: $@"

## Upload Ledger package to Bintray
upload: $(AAR_PATH)
	@echo "Uploading Ledger build..."
	@( test -n "$(BINTRAY_USER)" || echo "error: BINTRAY_USER is not set" ) && \
	 ( test -n "$(BINTRAY_API_KEY)" || echo "error: BINTRAY_API_KEY is not set" ) && \
	 ( test -n "$(BINTRAY_USER)" -a -n "$(BINTRAY_API_KEY)" )
	@docker run --rm -v $(PWD):$(DOCKER_PATH) \
	    -e BINTRAY_USER=$(BINTRAY_USER) \
		-e BINTRAY_API_KEY=$(BINTRAY_API_KEY) \
	    $(DOCKER_IMAGE) \
	    /bin/bash -c "cd $(DOCKER_PATH) && ./gradlew --no-daemon bintrayUpload"

## Start interactive Docker session
docker-shell: $(BUILD)/.check-submodules $(BUILD)/.docker-image
	@docker run -it --rm -v $(PWD):$(DOCKER_PATH) $(DOCKER_IMAGE) || true

LEDGER_SOURCE_TREE = ledger/ledger
LEDGER_PATCHES_DIR = ledger/patches

## Apply patches to Ledger source code
apply-patches: $(BUILD)/.patch-ledger

$(BUILD)/.patch-ledger:
	@cd $(LEDGER_SOURCE_TREE) && git am --committer-date-is-author-date $(abspath $(LEDGER_PATCHES_DIR))/*
	@mkdir -p $(@D)
	@touch $@

## Unapply patches to Ledger source code
unapply-patches:
	@git submodule update -- $(LEDGER_SOURCE_TREE)
	@rm -f $(BUILD)/.patch-ledger

## Snapshot current diff in Ledger source code
refresh-patches:
	@origin=$$(git submodule status --cached $(LEDGER_SOURCE_TREE) | awk '{print $$1}' | cut -c 2-); \
	 cd $(LEDGER_SOURCE_TREE) && git format-patch -o $(abspath $(LEDGER_PATCHES_DIR)) $$origin
