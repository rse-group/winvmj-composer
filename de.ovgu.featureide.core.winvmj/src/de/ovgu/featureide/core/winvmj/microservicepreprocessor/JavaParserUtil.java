package de.ovgu.featureide.core.winvmj.microservicepreprocessor;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

public class JavaParserUtil {

    private static final JavaParser JAVA_PARSER;

    static {
        ParserConfiguration parserConfiguration = new ParserConfiguration();
        parserConfiguration.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        JAVA_PARSER = new JavaParser(parserConfiguration);
    }

    public static CompilationUnit parse(IFile javaFile) {
        try {
            ParseResult<CompilationUnit> result = JAVA_PARSER.parse(javaFile.getContents());

            if (result.isSuccessful() && result.getResult().isPresent()) {
                return result.getResult().get();
            } else {
                throw new RuntimeException("Parsing failed: " + result.getProblems());
            }

        } catch (CoreException e) {
            throw new RuntimeException("File not found: " + javaFile.getFullPath(), e);
        }
    }
}