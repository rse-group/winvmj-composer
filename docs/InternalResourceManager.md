# Internal Resource Manager

InternalResourceManager is an utlity class used to export assets as a whole subdirectory of `resources`. The main motivation of this class is that exporting resources as a project and as a JAR plugin have different methods, so this class encapsulate those methods to ease the export process. This class has thre methods.
- `loadResourceDirectory`: public method that can be used in code to export the resources. There are two parameters:
  - `resourceDirPath`: resource location relative to `resources` directory of the plugin project.
  - `outPath`: absolute location of where the resource wants to be exported.
- `loadJarResource`: method to specifically export resources as a JAR plugin. Inspired by this [code](https://stackoverflow.com/questions/11012819/how-can-i-access-a-folder-inside-of-a-resource-folder-from-inside-my-jar-file).