param (
    [switch]$clean,
    [switch]$distclean,
    [switch]$install,
    [switch]$test
)

#$VERSION = (git describe --always --tags)
$VERSION = "v0.5.0"
$SRC_DIR = "src/com/shmuelzon/HomeAssistantFloorPlan"
$BUILD_DIR = "build/com/shmuelzon/HomeAssistantFloorPlan"
$DL_DIR = "dl"
$SWEET_HOME_VERSION = "7.3"
$SWEET_HOME_JAR = "$DL_DIR/SweetHome3D-$SWEET_HOME_VERSION.jar"
$J3D_CORE_JAR = "$DL_DIR/j3dcore.jar"
$J3D_VECMATH_JAR = "$DL_DIR/vecmath.jar"
$PLUGIN = "HomeAssistantFloorPlanPlugin-$VERSION.sh3p"

function DownloadFile {
    param (
        [string]$url,
        [string]$output
    )
    Write-Host "Downloading $output from $url..."
    Invoke-WebRequest -Uri $url -OutFile $output
}

if (-Not (Test-Path $DL_DIR)) {
    New-Item -ItemType Directory -Force -Path $DL_DIR
}

if (-Not (Test-Path $BUILD_DIR)) {
    New-Item -ItemType Directory -Force -Path $BUILD_DIR
}

#DownloadFile -url "https://sourceforge.net/projects/sweethome3d/files/SweetHome3D/SweetHome3D-$SWEET_HOME_VERSION/SweetHome3D-$SWEET_HOME_VERSION.jar" -output $SWEET_HOME_JAR
DownloadFile -url "https://jogamp.org/deployment/java3d/1.6.0-final/j3dcore.jar" -output $J3D_CORE_JAR
DownloadFile -url "https://jogamp.org/deployment/java3d/1.6.0-final/vecmath.jar" -output $J3D_VECMATH_JAR

$JAVA_FILES = Get-ChildItem -Path $SRC_DIR -Filter *.java
foreach ($file in $JAVA_FILES) {
    $outputFile = $file.FullName.Replace("src\", "build\").Replace(".java", ".class")
    if (-Not (Test-Path $outputFile)) {
        javac -classpath "$DL_DIR/*;build" --release 8 -d build $file.FullName
    }
}

$propertiesFile = "$SRC_DIR/ApplicationPlugin.properties"
$destPropertiesFile = "$BUILD_DIR/ApplicationPlugin.properties"
(Get-Content $propertiesFile) -replace '\${VERSION}', $VERSION | Set-Content $destPropertiesFile

& "$Env:JAVA_HOME\bin\jar.exe" -cf $PLUGIN -C build .

function Clean {
    if (Test-Path build) {
        Remove-Item -Recurse -Force build
    }
    if (Test-Path *.sh3p) {
        Remove-Item -Force *.sh3p
    }
}

function DistClean {
    Clean
    if (Test-Path dl) {
        Remove-Item -Recurse -Force dl
    }
}

function Install {
    $pluginDir = "$env:USERPROFILE\AppData\Roaming\eTeks\Sweet Home 3D\plugins"
    if (-Not (Test-Path $pluginDir)) {
        New-Item -ItemType Directory -Force -Path $pluginDir
    }
    Copy-Item -Force $PLUGIN -Destination $pluginDir
}

function Test-Install {
    Install
    java -jar $SWEET_HOME_JAR
}

if ($clean) {
    Clean
}

if ($distclean) {
    DistClean
}

if ($install) {
    Install
}

if ($test) {
    Test-Install
}

if (-Not ($clean -or $distclean -or $install -or $test)) {
    foreach ($file in $JAVA_FILES) {
        $outputFile = $file.FullName.Replace("src\", "build\").Replace(".java", ".class")
        javac -classpath "$DL_DIR/*;build" --release 8 -d build $file.FullName
    }
    & "$Env:JAVA_HOME\bin\jar.exe" -cf $PLUGIN -C build .
}