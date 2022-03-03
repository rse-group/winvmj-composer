# WinVMJ Composer Plugin

A plugin consisting the implementation of WinVMJ as a FeatureIDE Composer. Used for SPLE development using Delta-Oriented Programming approach.

## Getting started

### Prerequisites

To use this plugin, you need to install:

1. [Java 11](https://www.oracle.com/java/technologies/javase/jdk11-archive-downloads.html).
2. [Eclipse IDE 4.18 / 2020-12](https://www.eclipse.org/downloads/packages/release/2020-12/r).
3. [FeatureIDE 3.8.1](https://featureide.github.io/).
4. [Eclipse PDE](https://www.eclipse.org/pde/) if hasn't been included on downloaded IDE.
5. [PostgreSQL](https://www.postgresql.org/download/).
6. (Optional) PostgreSQL management tools such as [phpmyadmin](https://www.phpmyadmin.net/downloads/) or [adminer](https://www.adminer.org/).

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
├── db_and_routing.json
├── feature_to_module.json
└── model.xml
```
Each component can be explained as:
- `root`: project root
- `[src]`: generated product directory consist of selected modules and a generated product module. Its name can be defined by user when creating the project. Its content changes depending on selected configuration.
- `src-gen`: compiled generated product directory. All compiled products will be preserved in this directory.
- `[config]`: directory where product configurations are defined. Its name can be defined by user when creating the project and it will determined the product's name.
- `external`: directory where additional libraries are stored. Make sure the library's name is same as the its package name when being declared on module-info.
- `mappers`: directory where all database mapping of modules are stored.
- `modules`: directory where all implementation modules are stored.
- `db_and_routing.json`: a file that defines the mapping between features and modules. This is the example of its content.
```json
{
	"dataModel": [
		"aisco.program.core",
		"aisco.program.activity",
		"aisco.program.operational",
	],
	"methodRouting": [
		"aisco.program.activity",
		"aisco.financialreport.core",
	]
}
```
As seen above, it consist of two `key-value` pairs. The first `key` is `dataModel`, that defines which modules need a data table. The second `key` is `methodRouting`, that dfines which modules need routings.
- `feature_to_module.json`: a file that defines which modules require database and / or routing. This is the example of its content.
```json
{
	"Program": ["aisco.program.core"],
	"Activity": ["aisco.program.activity"],
	"Operational": ["aisco.program.operational"],
	"Income | Expense": ["aisco.chartofaccount.core","aisco.financialreport.core"],
}
```
As seen above, it consist of `key-value` pairs. The `key` is the feature condition required to apply a set of modules defined as `value`. The `key` can be a single feature or a mathematical logic with a set of features as its atoms.
- `model.xml`: a file that defines the feature model.

#### Development on FeatureIDE Project

We provide [WinVMJ-AISCO FeatureIDE Project](https://gitlab.com/RSE-Lab-Fasilkom-UI/PricesIDE/winvmj-composer/-/releases/) to help you simulate the development using this plugin, but if you want to create a new one:
1. On top toolbar, click on `File`>`New`>`Other...`. On wizard, select `FeatureIDE`>`FeatureIDE Project`.
2. Click `next`, and select `WinVMJ` on `Composer` dropdown. On this wizard, you can define your `[src]` and `[config]` name.
3. Click `next`, and name as well as locate your project.
4. Click `Finish`, and finally you'll find your project on package explorer.

To compose a product, you need to develop these artifacts:
- FeatureIDE assets such as `[config]` and `model.xml`. Please refer to [FeatureIDE guide](https://featureide.github.io/) to develop those artifacts.
- WinVMJ assets such as `modules` and `mappers`. Please refer to [Hibernate-Integrated WinVMJ Repository](https://gitlab.com/RSE-Lab-Fasilkom-UI/PricesIDE/vmj-aisco/-/tree/Hibernate-Integration) to develop those artifacts.
- Additional config file such as `db_and_routing.json` and `feature_to_module.json`. Please refer to example above to develop those artifacts.

Optionally, you can also import external libraries and store on `external`. Please be mind to the library naming as mentioned above.

The generated product is located on `[src]`, and as mentioned above, its content will change whenever the current feature selection changed. You can make adjustment on generated product to customize the detail that cannot be covered by this composer.

#### Compiling a product
If you feel the generated product is ready, you can compile it by:
1. Right click on project folder, then select `FeatureIDE`>`WinVMJ`>`Compile`.
2. Please wait until the compilation process is completed. You can check the `WinVMJ Console` to track this process.

#### Running a product
Before you run the product, you may need to setup the `hibernate.cfg.xml` located on `src-gen/[Product Name]/[product module name]` and `run.[bat/sh]` located on `src-gen/[Product Name]`, as your Postgresql credentials may differ from the generated one. The location of bot credentials are as follows:

`hibernate.cfg.xml`

`run.[bat/sh]`: line 1.
```bat
echo ... | psql "postgresql://[username]:[password]@localhost"
...
```

`hibernate.cfg.xml`: line 19-20.
```
...
<hibernate-configuration>

    <session-factory>
        ...
        <property name="connection.username">postgres</property>
        <property name="connection.password">root</property>
        ...
    </session-factory>

</hibernate-configuration>
```

After adjusting your credentials, we can run the product by following these steps:
1. On top toolbar, click on `Run`>`External Tools`>`External Tool Configuration`.
2. Select the script `Location` at `src-gen/[Product Name]/run.[bat (Windows) / sh (Linux / Mac)]`.
3. Select the `Working Directory` at `src-gen/[Product Name]/`.
4. Click `Run`.

This configuration will be saved and can be re-executed by clicking `Run`>`External Tools`>`[Your config name]`.

#### About Authorization
You need to seed the authorized user, which is managed on `sqlite` database. If you haven't run the product, please run it first so that it can generate the `db` file. The generated file has a name pattern: `[spl name]_product_[product name]`. We provide a auth seeding artifacts that consist of SQL file ``auth_seed.sql``, `sqlite` executable, SQL execution script `read_sql.bat` for Windows and `read_sql.sh` for Linux / Mac on the [release page](https://gitlab.com/RSE-Lab-Fasilkom-UI/PricesIDE/winvmj-composer/-/releases/). Unpack these artifact to `src-gen/[Desired Product Name]/`. To create a new user, please add this SQL statement on ``auth_seed.sql``:
```
INSERT INTO auth_user (id,password,allowedPermissions,name,email) VALUES ([unique integer],'fd4f97ae96ed4c0268d0b275765c849ce511419d96d6290ed583b9516f8cab61dfeddf43a522167bc9fa1eaeebb72b88158a2e646d1006799eb65a0e5805341a','',[Nama anda],[email google anda]);
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

To seed using those resource, you can create an `External Configuration` similar to how you run a product.
1. On top toolbar, click on `Run`>`External Tools`>`External Tool Configuration`.
2. Select the script `Location` at `src-gen/[Product Name]/auth_seed.sql`.
3. Select the `Working Directory` at `src-gen/[Product Name]/`.
4. Add these folloing `Arguments`: `[sqlite db file name] auth_seed.sql` on argument section.
5. Click `Run`.

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
