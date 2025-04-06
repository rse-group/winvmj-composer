package de.ovgu.featureide.core.winvmj.microservicepreprocessor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.modules.ModuleRequiresDirective;
import com.github.javaparser.ast.modules.ModuleOpensDirective;

public class ModuleInfoModifier {
    private static final String messagingModule = "vmj.messaging";

    public static void modifyModuleInfo(CompilationUnit cu){
        cu.findFirst(ModuleDeclaration.class).ifPresent(module -> {
            String opensModule = module.getNameAsString();

            // requires
            boolean requiresExists = module.getDirectives().stream()
                    .anyMatch(d -> d instanceof ModuleRequiresDirective &&
                            ((ModuleRequiresDirective) d).getNameAsString().equals(messagingModule));

            if (!requiresExists) {
                ModuleRequiresDirective requiresDirective = new ModuleRequiresDirective();
                requiresDirective.setName(new Name(messagingModule));
                module.addDirective(requiresDirective);
            }

            // opens to
            module.getDirectives().stream()
                    .filter(d -> d instanceof ModuleOpensDirective)
                    .map(d -> (ModuleOpensDirective) d)
                    .filter(d -> d.getNameAsString().equals(opensModule))
                    .findFirst()
                    .ifPresentOrElse(
                            opensDirective -> {
                                if (opensDirective.getModuleNames().stream().noneMatch(m -> m.asString().equals(messagingPackage))) {
                                    opensDirective.getModuleNames().add(new Name(messagingModule));
                                }
                            },
                            () -> {
                                ModuleOpensDirective newOpensDirective = new ModuleOpensDirective();
                                newOpensDirective.setName(new Name(opensModule));
                                newOpensDirective.getModuleNames().add(new Name(messagingModule));
                                module.addDirective(newOpensDirective);
                            }
                    );
        });

    }
}
