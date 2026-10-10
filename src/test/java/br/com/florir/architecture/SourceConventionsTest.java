package br.com.florir.architecture;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.source.tree.*;
import com.sun.source.util.*;
import java.nio.file.*;
import java.util.*;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

class SourceConventionsTest {
  @Test
  void variablesAndParametersUseCompleteNames() throws Exception {
    var violations = new ArrayList<String>();
    var compiler = ToolProvider.getSystemJavaCompiler();
    try (var paths = Files.walk(Path.of("src"))) {
      for (var sourcePath : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
        try (var fileManager = compiler.getStandardFileManager(null, null, null)) {
          var parsingTask =
              (JavacTask)
                  compiler.getTask(
                      null,
                      fileManager,
                      null,
                      List.of("-proc:none"),
                      null,
                      fileManager.getJavaFileObjects(sourcePath));
          for (var compilationUnit : parsingTask.parse())
            new TreeScanner<Void, Void>() {
              @Override
              public Void visitVariable(VariableTree variable, Void ignored) {
                String variableName = variable.getName().toString();
                if ((variableName.length() < 3 && !variableName.equals("id"))
                    || Set.of("req", "res", "aud", "env", "auth", "pr", "ac", "au", "pw", "catId")
                        .contains(variableName))
                  violations.add(sourcePath.getFileName() + ": " + variableName);
                return super.visitVariable(variable, ignored);
              }
            }.scan(compilationUnit, null);
        }
      }
    }
    assertTrue(violations.isEmpty(), String.join("\n", violations));
  }

  @Test
  void lombokIsConfiguredForCompileTimeGeneration() throws Exception {
    String buildConfiguration = Files.readString(Path.of("build.gradle"));
    assertTrue(buildConfiguration.contains("compileOnly 'org.projectlombok:lombok'"));
    assertTrue(buildConfiguration.contains("annotationProcessor 'org.projectlombok:lombok'"));
  }

  @Test
  void modelsUseLombokWithoutRecursiveDataAnnotations() throws Exception {
    try (var paths = Files.walk(Path.of("src/main/java/br/com/florir/model"))) {
      for (var sourcePath : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
        String source = Files.readString(sourcePath);
        assertTrue(source.contains("@Getter"), sourcePath.toString());
        assertFalse(source.contains("@Data"), sourcePath.toString());
      }
    }
  }

  @Test
  void generatedEntityAccessorsKeepExistingContracts() {
    var product = new br.com.florir.model.Product();
    product.setActive(true);
    product.setStock(7);
    assertTrue(product.getActive());
    assertEquals(7, product.getStock());
    var identifier = new br.com.florir.model.ProductPromotionId(1L, 2L);
    assertEquals(identifier, new br.com.florir.model.ProductPromotionId(1L, 2L));
    assertNotEquals(identifier, new br.com.florir.model.ProductPromotionId(2L, 1L));
  }
}
