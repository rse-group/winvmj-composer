# WinVMJ Composer Plugin

A plugin consisting the implementation of WinVMJ as a FeatureIDE Composer. Used for SPLE development using Delta-Oriented Programming approach.

## Getting started

### Prerequisites

Terdapat beberapa tools yang pelru anda install sebelum mencoba tools ini:

1. [Java 11](https://www.oracle.com/java/technologies/javase/jdk11-archive-downloads.html).
2. [Eclipse IDE 4.18 / 2020-12](https://www.eclipse.org/downloads/packages/release/2020-12/r).
3. [FeatureIDE 3.8.1](https://featureide.github.io/).
4. [Eclipse PDE](https://www.eclipse.org/pde/) if hasn't been included on downloaded IDE.

### Installing the Plugin

To install this plugin:

1. Download the zip releases and unzip it somehere. Check [Releases page](https://gitlab.com/RSE-Lab-Fasilkom-UI/PricesIDE/winvmj-composer/-/releases) to download the plugin zip. It's a package with pattern name: `de.ovgu.featureide.core.winvmj-vx.y.z`.
2. On top toolbar, click on `Help`>`Install New Software`.
2. Click on `Add`>`Archive`.
4. Select the zip file you downloaded before.
5. Select `WinVMJ Composer`. You can uncheck the `Group By Category` and `Contact all update sites` to avoid additional updates.
6. Click `Finish` or `Next`. You might get a notification about untrusted plugin. If so, click `Install Anyway`.
7. After the plugin has been installed, you will be asked to restart you plugin. Click `Restart Now`.
8. Congratulations! Now you can use FeatureIDE to develop software with WinVMJ.

### WinVMJ on FeatureIDE 101
Coming soon. In the meantime, feel free to visit [FeatureIDE](https://featureide.github.io/) page and try [WinVMJ-AISCO FeatureIDE Project](https://gitlab.com/RSE-Lab-Fasilkom-UI/PricesIDE/winvmj-composer/-/releases/v0.0.1) to learn more about its usage.

#### WinVMJ FeatureIDE Structure
```
root
├── [src]
├── src-gen
│   ├── pl.product.product1/
│	├── pl.product.product2/
│	├── ...
│	└── pl.product.productn/
├── [config]
│   ├── config1.xml
│	├── config2.xml
│	├── ...
│	└── confign.xml
├── libs
│   ├── lib1.jar
│	├── lib2.jar
│	├── ...
│	└── libn.jar
├── modules
│   ├── pl.corefeature1.core/
│	├── pl.corefeature1.delta1/
│	├── pl.corefeature1.delta2/
│	├── ...
│	└── pl.corefeaturem.deltan/
├── db_and_routing.json
├── feature_to_module.json
└── model.xml
```
Each component can be explained as:
- `root`: project root
- `[src]`: generated product directory. Its name can be defined by user when creating the project. Its content changes depending on selected configuration.
- `src-gen`: compiled generated product directory. All compiled products will be preserved in this directory.
- `[config]`: directory where product configurations are defined. Its name can be defined by user when creating the project.
- `libs`: directory where additional libraries are stored. For now, all external library will be imported while compiling a product.
- `modules`: directory where all implementation modules are stored.
- `db_and_routing.json`: a file that defines the mapping between features and modules.
- `feature_to_module.json`: a file that defines which modules require database and / or routing.
- `model.xml`: a file that defines the feature model.

### Developer's Corner

This section explains about how to develop this plugin.

#### Opening Plugin Projects

1. Open your `Eclipse IDE`.
2. Select `File`>`Open projects from file system...`.
3. Click `Directory`, and then select where the `de.ovgu.featureide.core.winvmj` directory of this repository is located.
4. If there are error markers, please add the `Plug-In Dependiencies` manually by:
  - Right-click the project directory and click `Build Path`>`Configure Build Path`.
  - Select the `Library`>`Classpath` section and click `add library` button.
  - Select `Plug-In Dependencies` and click `next`>`Finish`.
  
