#!/usr/bin/env bash
#
# Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
#
# Licensed under the MIT License.
#

set -o errexit
set -o errtrace
set -o pipefail
set -o nounset

readonly SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")/" && pwd)"
readonly BASE_DIRECTORY="$(dirname "${SCRIPT_DIRECTORY}")"

# Load metadata from suite.py
readonly py_export=$(cat <<-END
from suite import suite;
vars= ' '.join(['DEP_%s=%s' % (k.upper(), v)
  for k, v in suite['trufflesqueak:dependencyMap'].items()]);
slug = '/'.join(suite['url'].split('/')[-2:]);
mxversion = suite['mxversion']
print('export %s GITHUB_SLUG=%s MX_VERSION=%s' % (vars, slug, mxversion))
END
)
$(cd "${SCRIPT_DIRECTORY}" && python -c "${py_export}")
([[ -z "${DEP_GRAALVM_TAG}" ]] || [[ -z "${GITHUB_SLUG}" ]]) && \
  echo "Failed to load values from dependencyMap and GitHub slug." 1>&2 && exit 1

OS_NAME=$(uname -s | tr '[:upper:]' '[:lower:]')
[[ "${OS_NAME}" == msys* || "${OS_NAME}" == cygwin* || "${OS_NAME}" == mingw* ]] && OS_NAME="windows"
OS_ARCH="amd64"
[[ "${OS_NAME}" == "linux" ]] && [[ "$(dpkg --print-architecture)" == "arm64" ]] && OS_ARCH="aarch64"
JAVA_HOME_SUFFIX="" && [[ "${OS_NAME}" == "darwin" ]] && JAVA_HOME_SUFFIX="/Contents/Home"
readonly OS_NAME OS_ARCH JAVA_HOME_SUFFIX


add-path() {
  echo "$(resolve-path "$1")" >> $GITHUB_PATH
}

build-component() {
  local env_name=$1
  local component_name=$2
  local target=$3
  local graalvm_home="$(mx --env "${env_name}" graalvm-home)"

  mx --env "${env_name}" build
  cp $(mx --env "${env_name}" paths "${component_name}") "${target}"

  add-path "${graalvm_home}/bin"
  set-env "GRAALVM_HOME" "$(resolve-path "${graalvm_home}")"
  echo "[${graalvm_home} set as \$GRAALVM_HOME]"
}

deploy-asset() {
  local git_tag=$(git tag --points-at HEAD)
  if [[ -z "${git_tag}" ]]; then
    echo "Skipping deployment step (commit not tagged)"
    exit 0
  elif ! [[ "${git_tag}" =~ ^[[:digit:]] ]]; then
    echo "Skipping deployment step (tag ${git_tag} does not start with a digit)"
    exit 0
  fi
  local filename=$1
  local auth="Authorization: token $2"
  local release_id

  tag_result=$(curl -L --retry 3 --retry-connrefused --retry-delay 2 -sH "${auth}" \
    "https://api.github.com/repos/${GITHUB_SLUG}/releases/tags/${git_tag}")
  
  if echo "${tag_result}" | grep -q '"id":'; then
    release_id=$(echo "${tag_result}" | grep '"id":' | head -n 1 | sed 's/[^0-9]*//g')
    echo "Found GitHub release #${release_id} for ${git_tag}"
  else
    # Retry (in case release was just created by some other worker)
    tag_result=$(curl -L --retry 3 --retry-connrefused --retry-delay 2 -sH "${auth}" \
    "https://api.github.com/repos/${GITHUB_SLUG}/releases/tags/${git_tag}")
  
    if echo "${tag_result}" | grep -q '"id":'; then
      release_id=$(echo "${tag_result}" | grep '"id":' | head -n 1 | sed 's/[^0-9]*//g')
      echo "Found GitHub release #${release_id} for ${git_tag}"
    else
      create_result=$(curl -sH "${auth}" \
        --data "{\"tag_name\": \"${git_tag}\",
                \"name\": \"${git_tag}\",
                \"body\": \"\",
                \"draft\": false,
                \"prerelease\": false}" \
        "https://api.github.com/repos/${GITHUB_SLUG}/releases")
      if echo "${create_result}" | grep -q '"id":'; then
        release_id=$(echo "${create_result}" | grep '"id":' | head -n 1 | sed 's/[^0-9]*//g')
        echo "Created GitHub release #${release_id} for ${git_tag}"
      else
        echo "Failed to create GitHub release for ${git_tag}"
        exit 1
      fi
    fi
  fi

  curl --fail -o /dev/null -w "%{http_code}" \
    -H "${auth}" -H "Content-Type: application/zip" \
    --data-binary @"${filename}" \
    "https://uploads.github.com/repos/${GITHUB_SLUG}/releases/${release_id}/assets?name=${filename}"
}

download-asset() {
  local filename=$1
  local git_tag=$2
  local target="${3:-$1}"

  curl -s -L --retry 3 --retry-connrefused --retry-delay 2 -o "${target}" \
    "https://github.com/${GITHUB_SLUG}/releases/download/${git_tag}/${filename}"
}

download-trufflesqueak-icon() {
  local target="${BASE_DIRECTORY}/src/de.hpi.swa.trufflesqueak/src/de/hpi/swa/trufflesqueak/io/trufflesqueak-icon.png"

  if ls -1 ${target} 2>/dev/null; then
    echo "[TruffleSqueak icon already downloaded]"
    return
  fi

  download-asset "${DEP_ICON}" "${DEP_ICON_TAG}" "${target}"
  echo "[TruffleSqueak icon (${DEP_ICON_TAG}) downloaded successfully]"
}

download-trufflesqueak-image() {
  local target_dir="${BASE_DIRECTORY}/src/resources"

  if ls -1 ${target_dir}/*.image 2>/dev/null; then
    echo "[TruffleSqueak image already downloaded]"
    return
  fi

  pushd "${target_dir}" > /dev/null

  download-asset "${DEP_IMAGE}" "${DEP_IMAGE_TAG}"
  unzip -qq "${DEP_IMAGE}"
  rm -f "${DEP_IMAGE}"

  popd > /dev/null

  echo "[TruffleSqueak image (${DEP_IMAGE_TAG}) downloaded successfully]"
}

enable-jdk() {
  add-path "$1/bin"
  set-env "JAVA_HOME" "$(resolve-path "$1")"
}

download-trufflesqueak-test-image() {
  local target_dir="${BASE_DIRECTORY}/images"

  if [[ -f "${target_dir}/test-64bit.image" ]]; then
    echo "[TruffleSqueak test image already downloaded]"
    return
  fi

  mkdir "${target_dir}" || true
  pushd "${target_dir}" > /dev/null

  download-asset "${DEP_TEST_IMAGE}" "${DEP_TEST_IMAGE_TAG}"
  unzip -qq "${DEP_TEST_IMAGE}"
  mv ./*.image test-64bit.image
  mv ./*.changes test-64bit.changes

  popd > /dev/null

  echo "[TruffleSqueak test image (${DEP_TEST_IMAGE_TAG}) downloaded successfully]"
}

installable-filename() {
  local java_version=$1
  local svm_prefix=$2
  local git_describe=$(git describe --tags --always)
  local git_short_commit=$(git log -1 --format="%h")
  local git_description="${git_describe:-${git_short_commit}}"
  echo "trufflesqueak-installable${svm_prefix}-${java_version}-${OS_NAME}-${OS_ARCH}-${git_description}.jar"
}

resolve-path() {
  if [[ "${OS_NAME}" == "windows" ]]; then
    # Convert Unix path to Windows path
    echo "$1" | sed 's/\/c/C:/g' | sed 's/\//\\/g'
  else
    echo "$1"
  fi
}

set-env() {
  echo "$1=$2" >> $GITHUB_ENV
  echo "export $1=\"$2\"" >> "${HOME}/all_env_vars"
}

set-up-dependencies() {
  local java_version=$1

  case "$(uname -s)" in
    "Linux")
      sudo apt update -qq && sudo apt install -qq libsdl2-dev
      ;;
    "Darwin")
      HOMEBREW_NO_AUTO_UPDATE=1 brew install sdl2
      ;;
  esac

  # Repository was shallow copied and Git did not fetch tags, so fetch the tag
  # of the commit (if any) to make it available for other Git operations.
  git -c protocol.version=2 fetch --prune --progress --no-recurse-submodules \
    --depth=1 origin "+$(git rev-parse HEAD):refs/remotes/origin/master"

  set-up-mx
  shallow-clone-graalvm-project https://github.com/oracle/graal.git
  shallow-clone-graalvm-project https://github.com/graalvm/graaljs.git
  download-trufflesqueak-image
  download-trufflesqueak-test-image
  download-trufflesqueak-icon

  case "${java_version}" in
    "java8")
      set-up-labsjdk8 "${HOME}"
      ;;
    "java11")
      set-up-labsjdk11 "${HOME}"
      ;;
    "java16")
      set-up-labsjdk16 "${HOME}"
      ;;
    *)
      echo "Failed to set up ${java_version}"
      exit 42
      ;;
  esac

  set-env "INSTALLABLE_JVM_TARGET" "$(installable-filename "${java_version}" "")"
  set-env "INSTALLABLE_SVM_TARGET" "$(installable-filename "${java_version}" "-svm")"
}

set-up-labsjdk() {
  local target_dir=$1
  local jdk_tar=${target_dir}/jdk.tar.gz
  local jdk_name=$2
  local jdk_base_url=$3
  local jdk_name_extracted=$4

  pushd "${target_dir}" > /dev/null
  curl -sSL --retry 3 -o "${jdk_tar}" "${jdk_base_url}/${DEP_JVMCI}/${jdk_name}.tar.gz"
  tar xzf "${jdk_tar}"
  popd > /dev/null

  enable-jdk "${target_dir}/${jdk_name_extracted}"
  echo "[${jdk_name} set up successfully]"
}

set-up-labsjdk8() {
  set-up-labsjdk $1 \
    "openjdk-8u${DEP_JDK8}+${DEP_JDK8_UPDATE}-${DEP_JVMCI}-${OS_NAME}-${OS_ARCH}" \
    "https://github.com/graalvm/graal-jvmci-8/releases/download" \
    "openjdk1.8.0_${DEP_JDK8}-${DEP_JVMCI}${JAVA_HOME_SUFFIX}"
}

set-up-labsjdk11() {
  set-up-labsjdk $1 \
    "labsjdk-ce-${DEP_JDK11}+${DEP_JDK11_UPDATE}-${DEP_JVMCI}-${OS_NAME}-${OS_ARCH}" \
    "https://github.com/graalvm/labs-openjdk-11/releases/download" \
    "labsjdk-ce-${DEP_JDK11}-${DEP_JVMCI}${JAVA_HOME_SUFFIX}"
}

set-up-labsjdk16() {
  set-up-labsjdk $1 \
    "labsjdk-ce-${DEP_JDK16}+${DEP_JDK16_UPDATE}-${DEP_JVMCI}-${OS_NAME}-${OS_ARCH}" \
    "https://github.com/graalvm/labs-openjdk-16/releases/download" \
    "labsjdk-ce-${DEP_JDK16}-${DEP_JVMCI}${JAVA_HOME_SUFFIX}"
}

set-up-mx() {
  shallow-clone "https://github.com/graalvm/mx.git" "${MX_VERSION}" "${HOME}/mx"
  add-path "${HOME}/mx"
  set-env "MX_HOME" "${HOME}/mx"
  echo "[mx (${MX_VERSION}) set up successfully]"
}

shallow-clone() {
  local git_url=$1
  local git_commit_or_tag=$2
  local target_dir=$3

  mkdir "${target_dir}" || true
  pushd "${target_dir}" > /dev/null

  git init > /dev/null
  git remote add origin "${git_url}"
  git fetch --quiet --depth 1 origin "${git_commit_or_tag}"
  git reset --quiet --hard FETCH_HEAD

  popd > /dev/null
}

shallow-clone-graalvm-project() {
  local git_url=$1
  local name=$(basename "${git_url}" | cut -d. -f1)
  local target_dir="${BASE_DIRECTORY}/../${name}"

  shallow-clone "${git_url}" "${DEP_GRAALVM_TAG}" "${target_dir}"
}

$@
