import ca.ualberta.libmigproto.synthesis.SimpleSynthesizer;
import ca.ualberta.libmigproto.transformation.HardCodedTransformationRuleSet;
import ca.ualberta.libmigproto.util.Permutation;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Playground {
    String code = """
        var builder = CSVFormat.DEFAULT.builder();
        builder = builder.setDelimiter(p_0);
        builder = builder.setQuote(p_1);
        builder = builder.setRecordSeparator(p_2);
        var preferences = builder.build();
        var reader = new CSVParser(new StringReader(csv), preferences);
        var results = reader.getRecords();
        var firstRecord = results.get(p_3);
        var records = firstRecord.stream().toList();
        return records;
""";

    @Test
    public void parse() {
        var block = JavaParser.parseBlock("{" + code + "}");
        printBlock(block);
    }

    public void printBlock(BlockStmt blk) {
        for (var child : blk.getChildNodes()) {
            printNode(child, 0);
        }
    }

    public void printNode(Node node, int level) {
        System.out.println(" ".repeat(level * 2) + node.toString() + "  ::" + node.getClass().getSimpleName());
        for (var child : node.getChildNodes()) {
            printNode(child, level + 1);
        }
    }

    @Test
    public void applyTransformation() throws IOException {
        var ruleSet = new HardCodedTransformationRuleSet();
        var cu = JavaParser.parse(Path.of("D:\\PhD\\LibMigProto\\SampleCsvClient\\src\\main\\java\\SuperCsvClient.java.original"));
        var statements = cu.findAll(ExpressionStmt.class);

        var builderStatement = statements.get(0);
        var readerStatement = statements.get(2);
        var recordsStatement = statements.get(3);

        var parentBlock = (BlockStmt) builderStatement.getParentNode().get();
        var constants = parentBlock.findAll(LiteralExpr.class);
        constants.add(0, new IntegerLiteralExpr(0));

        Arrays.asList(recordsStatement, readerStatement, builderStatement).forEach(sourceApi -> {
            var rule =  ruleSet.getRule(sourceApi);
            rule.apply(parentBlock);
        });

        var holes = parentBlock.findAll(NameExpr.class)
                .stream()
                .filter(name -> name.getNameAsString().startsWith("p_"))
                .toList();
        var permutation = new Permutation(constants, holes.size());

        var found = false;

        while (permutation.hasNext() && !found) {
            var cloneParent = parentBlock.clone();
            parentBlock.replace(cloneParent);
            var fillers = permutation.next();
            System.out.println(fillers);
            new SimpleSynthesizer().fillHole(cloneParent, fillers);
            Files.write(Path.of("D:\\PhD\\LibMigProto\\SampleCsvClient\\src\\main\\java\\CsvMigration.java"), Collections.singleton(cu.toString()), StandardCharsets.UTF_8);
            if(runTest()) {
                found = true;
                break;
            }
            cloneParent.replace(parentBlock);
            Files.write(Path.of("D:\\PhD\\LibMigProto\\SampleCsvClient\\src\\main\\java\\CsvMigration.java"), Collections.singleton(cu.toString()), StandardCharsets.UTF_8);
        }

        Assertions.assertTrue(found);
    }

    public boolean runTest() throws IOException {
        String PATH_TO_GRADLE_PROJECT = "D:\\PhD\\LibMigProto\\SampleCsvClient\\";
        String GRADLEW_EXECUTABLE = "gradlew.bat";
        String GRADLE_TASK = "test";

        String command = PATH_TO_GRADLE_PROJECT + GRADLEW_EXECUTABLE + " " + GRADLE_TASK + " -p " + PATH_TO_GRADLE_PROJECT;
        var process = Runtime.getRuntime().exec(command);
        BufferedReader errinput = new BufferedReader(new InputStreamReader(
                process.getErrorStream()));

        var error = errinput.lines().collect(Collectors.joining());
        // System.err.println(error);

        return error == null || error.isEmpty();
    }

    @Test
    public void PermutationTest() {
        var perm = new Permutation(List.of(0, 1, 2), 2);
        while (perm.hasNext()) {
            var n = perm.next();
            System.out.println(n);
        }
    }
}
