# WinVMJ Composer Plugin

A plugin consisting the implementation of WinVMJ as a FeatureIDE Composer. Used for SPLE development using Delta-Oriented Programming approach.

## Getting started

### Prerequisites

To use this plugin, you need to install:

1. [Java 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html).
2. [Eclipse IDE 4.30 / 2023-12](https://www.eclipse.org/downloads/packages/release/2023-12/r).
3. [FeatureIDE 3.11.1](https://featureide.github.io/#download).
4. [Eclipse PDE](https://www.eclipse.org/pde/) if hasn't been included on downloaded IDE.
5. [PostgreSQL](https://www.postgresql.org/download/).
6. DBMS to manage PostgreSQL such as [phpmyadmin](https://www.phpmyadmin.net/downloads/) or [adminer](https://www.adminer.org/).

### Installing the Plugin

WinVMJ Composer can be installed via Releases or Update Site

#### Via Releases

1. Download the zip releases and unzip it somehere. Check [Releases page](https://gitlab.com/RSE-Lab-Fasilkom-UI/PricesIDE/winvmj-composer/-/releases) to download the plugin zip. It's a package with pattern name: `de.ovgu.featureide.core.winvmj-vx.y.z`.
2. On top toolbar, click on `Help`>`Install New Software`.
3. Click on `Add`>`Archive`.
4. Select the zip file you downloaded before.
5. Make sure you check the `Group Items by Category` first and the select `WinVMJ Composer`. You can uncheck `Contact all update sites` to avoid additional updates. If this is your first time installing the version 1.1.0 onwards, please select the `FeatureIDE Patch` too as those are patched plugin that enables UVL usage.
6. Click `Finish` or `Next`. You might get a notification about untrusted plugin. If so, click `Install Anyway`.
7. After the plugin has been installed, you will be asked to restart you plugin. Click `Restart Now`.
8. Congratulations! Now you can use FeatureIDE to develop software with WinVMJ.

#### Via Update Site

1. On top toolbar, click on `Help`>`Install New Software`.
2. Type PriceISE update site (https://amanah.cs.ui.ac.id/priceside/updatesite/) on `Work with` textbox.
3. Follow step 5 onwards from previous method. Because you work with PricesIDE update site, you might see other plugins such as `UML to VMJ`. You could install those plugins but please follow their respective installation guidelines.

### WinVMJ on FeatureIDE 101
We strongly recommend to visit [FeatureIDE Github Page](https://featureide.github.io/) and [WinVMJ-AISCO Gitlab Repository](https://gitlab.com/RSE-Lab-Fasilkom-UI/PricesIDE/vmj-aisco/-/tree/Hibernate-Integration) to learn more about respective framework's usage.

#### WinVMJ FeatureIDE Structure
```
root
├── [src]
│   ├── pl.corefeature1.core/
│   ├── pl.corefeature1.delta1/
│   ├── ...
│   └── pl.product.productx/
├── src-gen
│   ├── pl.product.product1/
│   ├── pl.product.product2/
│   ├── ...
│   └── pl.product.productn/
├── [config]
│   ├── config1.xml
│   ├── config2.xml
│   ├── ...
│   └── confign.xml
├── external
│   ├── lib1.jar
│   ├── lib2.jar
│   ├── ...
│   └── libn.jar
├── modules
│   ├── pl.corefeature1.core/
│   ├── pl.corefeature1.delta1/
│   ├── pl.corefeature1.delta2/
│   ├── ...
│   └── pl.corefeaturem.deltan/
├── db.properties
├── feature_to_module.json
└── model.uvl
```
Each component can be explained as:
- `root`: project root
- `[src]`: generated product directory consist of selected modules (including from other SPLs) and a generated product module. Its name can be defined by user when creating the project. Its content changes depending on selected configuration.
- `src-gen`: compiled generated product directory. All compiled products will be preserved in this directory.
- `[config]`: directory where product configurations are defined. Its name can be defined by user when creating the project and it will determined the product's name.
- `external`: directory where additional libraries are stored. Make sure the library's name is same as the its package name when being declared on module-info.
- `modules`: directory where all implementation modules are stored.
- `db.properties`: a file that defines the database credentials. This is the example of its content.
```properties
db.username=postgres
db.password=postgres
```
As seen above, it consist two properties: `db.username` that defines your `Postgresql` username and `db.password` that defines your `Postgresql` password. Please adjust it to match your current credentials.
- `feature_to_module.json`: a file that defines the Feature-Module mapping. This is the example of its content.
```json
{
	"Program": ["aisco.program.core"],
	"Activity": ["aisco.program.activity"],
	"Operational": ["aisco.program.operational"],
	"Income | Expense": ["aisco.chartofaccount.core","aisco.financialreport.core"],
}
```
As seen above, it consist of `key-value` pairs. The `key` is the feature condition required to apply a set of modules defined as `value`. The `key` can be a single feature or a mathematical logic with a set of features as its atoms.
- `model.uvl`: a file that defines the feature model.

#### Development on FeatureIDE Project

To create a new FeatureIDE project:
1. On top toolbar, click on `File`>`New`>`Other...`. On wizard, select `FeatureIDE`>`FeatureIDE Project`.
2. Click `next`, and select `WinVMJ` on `Composer` dropdown. On this wizard, you can define your `[src]` and `[config]` name.
3. Click `next`, and name as well as locate your project.
4. Click `Finish`, and finally you'll find your project on package explorer.

To compose a product, you need to develop these artifacts:
- FeatureIDE assets such as `[config]` and `model.uvl`. Please refer to [FeatureIDE guide](https://featureide.github.io/) to develop those artifacts.
- WinVMJ assets such as `modules`. Please refer to [Hibernate-Integrated WinVMJ Repository](https://gitlab.com/RSE-Lab-Fasilkom-UI/PricesIDE/vmj-aisco/-/tree/Hibernate-Integration) to develop those artifacts.
- Additional config file such as `db.properties` and `feature_to_module.json`. Please refer to example above to develop those artifacts.

Optionally, you can also import external libraries and store on `external`. Please be mind to the library naming as mentioned above.

The generated product is located on `[src]`, and as mentioned above, its content will change whenever the current feature selection changed. You can make adjustment on generated product to customize the detail that cannot be covered by this composer.

#### MPL Development

A FeatureIDE project that uses WinVMJ Composer can applicate MPL development by following these steps:
1. Right-click on project root, then select `Properties`.
2. Select `Project References`, then add another FeatureIDE project that uses WinVMJ Composer.
3. Create `interfaces` directory on project root.
4. Copy the referenced project's feature model and rename it based on its product line name (Root Feature). The copied feature model is called Feature Model Interface.
5. You can edit the feature model interface as long as the result fulfills these two conditions:
  - Its features is a subset of its original sources features.
  - Its possible configurations is a subset of its original source's possible configurations.
   Do mind that currently there's no such validation procedure to validate your edited feture model based on those two conditions. Such procedure is still an open question.
6. Integrate those feature model interfaces on `model.uvl` using sources editor (switch to `source` on the bottom tab when editing `model.uvl`): add the import statement with pattern `interfaces [PL Name]`, and then define the feature model interface's root location on `model.uvl`. Here is the example of the integration result.
```
namespace AISCO

imports
	interfaces.Blog as bo
features
	AISCO {abstract}	
		mandatory
			Program
			FinancialReport {abstract}	
				...
			bo.Blog
```
After that, you can develop the FeatureIDE project just like before, only this time you can reuse the external SPL assets by selecting feature model interfaces' features on configuration. You can even define a cross-tree constraint on `movel.uvl`, which is a constraint that involve features from different SPLs.
7. You can also create `inter_spl_product` to define the cross SPL product dependency, albeit you also need to manually validate it.

The MPL project should look like this.
```
root
├── [src]
│   ├── pl.corefeature1.core/
│   ├── pl.corefeature1.delta1/
│   ├── exspl1.corefeaturex.deltay/
│   ├── ...
│   └── pl.product.productx/
├── src-gen/
├── [config]/
├── external/
├── interfaces
│   ├── ExSPL1.uvl
│   ├── ExSPL2.uvl
│   ├── ...
│   └── ExSPLn.uvl
├── modules/
├── db.properties
├── feature_to_module.json
├── inter_spl_product.json
└── model.uvl
```
The additional component can be explained as:
- `interfaces`: directory where related SPL's feture model interface are stored.
- `inter_spl_product.json`: a file that defines the cross SPL product dependency.
This is the example of its content.
```json
{
	"SheilaPlusSchool": ["PaymentGateway:FullMidtrans", "Blog:Standard"],
	"SheilaSchool": ["PaymentGateway:FullDOKU"]
}
```
As seen above, it consist of `key-value` pairs. The `key` is the product names defined by the SPL, while the `value` is a list of products defined on related SPLs with the format `[External SPL name]:[Product Name]`.
- Additionally, the `[src]` directory now holds the external SPL's module if selected.

We provide [WinVMJ-AISCO-MPL FeatureIDE Project Set](https://gitlab.com/RSE-Lab-Fasilkom-UI/PricesIDE/winvmj-composer/-/releases/) to help you simulate the MPL development using this plugin. It consist of an MPL project and two related project. If the projects are somehow not linked, please link it manually by:
1. Right click on `WinVMJ-AISCO-MPL` project, then select `Properties`.
2. On left side of `properties` menu, please select `Project References` and then select both `WinVMJ-PaymentGateway` and `WinVMJ-Blog`.
3. Click `Apply and Close` to finish the process.

#### Compiling a product
If you feel the generated product is ready, you can compile it by:
1. Right click on project folder, then select `FeatureIDE`>`WinVMJ`>`Compile`.
2. Please wait until the compilation process is completed. You can check the `WinVMJ Console` to track this process.

#### Running a product

> Note: If you're on MacOS/Linux, run `chmod u+x run.sh ` inside the folder containing `run.sh` in src-gen before proceeding.

We can run the product by following these steps:
1. On top toolbar, click on `Run`>`External Tools`>`External Tool Configuration`.
2. Select the script `Location` at `src-gen/[Product Name]/run.[bat (Windows) / sh (Linux / Mac)]`.
3. Select the `Working Directory` at `src-gen/[Product Name]/`.
4. Click `Run`.

This configuration will be saved and can be re-executed by clicking `Run`>`External Tools`>`[Your config name]`.

Running a product for the first time also create a new database with name that match this pattern: [SPL Name]_product_[Product Name]. You can open this database using your DBMS.

#### Example Projects
We provide [WinVMJ-AISCO FeatureIDE Project](https://gitlab.com/RSE-Lab-Fasilkom-UI/PricesIDE/winvmj-composer/-/releases/) to help you simulate the development using this plugin.

#### About Authorization
We provide a auth seeding artifacts that consist of SQL file ``auth_seed.sql`` on the [release page](https://gitlab.com/RSE-Lab-Fasilkom-UI/PricesIDE/winvmj-composer/-/releases/). Unpack these artifact to `src-gen/[Desired Product Name]/`. You can use this file on your DBMS to seed the auth database.


To create a new user, please execute this SQL statement on your DBMS:
```
INSERT INTO auth_user_comp (id) VALUES ([unique integer]);
INSERT INTO auth_user_impl_passworded (id,password,allowedPermissions,name,email) VALUES ([unique integer],'fd4f97ae96ed4c0268d0b275765c849ce511419d96d6290ed583b9516f8cab61dfeddf43a522167bc9fa1eaeebb72b88158a2e646d1006799eb65a0e5805341a','',[Nama anda],[email google anda]);
```

For example:
```
INSERT INTO auth_user (id,password,allowedPermissions,name,email) VALUES (5,'fd4f97ae96ed4c0268d0b275765c849ce511419d96d6290ed583b9516f8cab61dfeddf43a522167bc9fa1eaeebb72b88158a2e646d1006799eb65a0e5805341a','','Samuel Tupa Febrian','samuel.febrian@gmail.com');
```

**Please be mind that `id` field is unique.**

Then, please assign your account role as needed by adding:
```
INSERT INTO auth_user_role (id,role,user) VALUES ([unique integer],[id role],[id user]);
```

For example:
```
INSERT INTO auth_user_role (id,role,user) VALUES (13,1,5);
```

### Developer's Corner

This section explains about how to develop this plugin.

#### Opening Plugin Projects

1. Open your `Eclipse IDE`.
2. Select `File`>`Open projects from file system...`.
3. Click `Directory`, and then select where the `de.ovgu.featureide.core.winvmj`, `de.ovgu.featureide.core.winvmj.feature`, and `de.ovgu.featureide.core.winvmj.updatesite` directories of this repository is located.

#### Important Classes
When developing this plugin, you will most likely develop these classes.
- [WinVMJCorePlugin](https://gitlab.com/RSE-Lab-Fasilkom-UI/PricesIDE/winvmj-composer/-/blob/plugin-dev/de.ovgu.featureide.core.winvmj/src/de/ovgu/featureide/core/winvmj/WinVMJCorePlugin.java): the activator class of this plugin.
- [WinVMJComposer](https://gitlab.com/RSE-Lab-Fasilkom-UI/PricesIDE/winvmj-composer/-/blob/plugin-dev/de.ovgu.featureide.core.winvmj/src/de/ovgu/featureide/core/winvmj/WinVMJComposer.java): the composer class of this plugin.
- [WinVMJFMComposer](https://gitlab.com/RSE-Lab-Fasilkom-UI/PricesIDE/winvmj-composer/-/blob/plugin-dev/de.ovgu.featureide.core.winvmj/src/de/ovgu/featureide/core/winvmj/WinVMJFMComposer.java): the FM composer class of this plugin. Implemented to disable feature order due to DOP approach.
- [WinVMJProduct](https://gitlab.com/RSE-Lab-Fasilkom-UI/PricesIDE/winvmj-composer/-/blob/plugin-dev/de.ovgu.featureide.core.winvmj/src/de/ovgu/featureide/core/winvmj/core/WinVMJProduct.java): a class that defines a WinVMJ product variation based on config or generated product.
- [SourceCompiler](https://gitlab.com/RSE-Lab-Fasilkom-UI/PricesIDE/winvmj-composer/-/blob/plugin-dev/de.ovgu.featureide.core.winvmj/src/de/ovgu/featureide/core/winvmj/compile/SourceCompiler.java): utility class used to compile a generated product into runnable JARs.
- [TemplateRenderer](https://gitlab.com/RSE-Lab-Fasilkom-UI/PricesIDE/winvmj-composer/-/blob/plugin-dev/de.ovgu.featureide.core.winvmj/src/de/ovgu/featureide/core/winvmj/templates/TemplateRenderer.java): abstract class that can be extended to generate artifacts with Freemarker Template Engine. All templates should be stored on [`resources/templates`](https://gitlab.com/RSE-Lab-Fasilkom-UI/PricesIDE/winvmj-composer/-/tree/plugin-dev/de.ovgu.featureide.core.winvmj/resources/templates).
- [InternalResourceManager](https://gitlab.com/RSE-Lab-Fasilkom-UI/PricesIDE/winvmj-composer/-/blob/plugin-dev/de.ovgu.featureide.core.winvmj/src/de/ovgu/featureide/core/winvmj/internal/InternalResourceManager.java): utlity class used to export assets from [`resources`](https://gitlab.com/RSE-Lab-Fasilkom-UI/PricesIDE/winvmj-composer/-/tree/plugin-dev/de.ovgu.featureide.core.winvmj/resources).
