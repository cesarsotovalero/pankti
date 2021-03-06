package se.kth.castor.pankti.launchers;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import picocli.CommandLine;
import se.kth.castor.pankti.logging.CustomLogger;
import se.kth.castor.pankti.processors.FirstMethodProcessor;
import se.kth.castor.pankti.processors.MethodProcessor;
import spoon.MavenLauncher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtType;

@CommandLine.Command(
    name = "java -jar target/<pankti-version-jar-with-dependencies.jar>",
    description = "pankti converts application traces to tests",
    usageHelpWidth = 100)
public class PanktiLauncher implements Callable<Integer> {

    private static final Logger LOGGER = CustomLogger.log(PanktiLauncher.class.getName());

    @CommandLine.Parameters(
        paramLabel = "PATH",
        description = "Path of the Maven project")
    private Path projectPath;

    @CommandLine.Option(
        names = {"-h", "--help"},
        description = "Display help/usage.",
        help = true)
    private boolean help = false;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new PanktiLauncher()).execute(args);
        System.exit(exitCode);
    }

    public PanktiLauncher(final Path projectPath, final boolean help) {
        this.projectPath = projectPath;
        this.help = help;
    }

    public PanktiLauncher() {
    }

    @Override
    public Integer call() {

        if (help) {
            LOGGER.info("Pankti version: " + "1.0-SNAPSHOT");
            return 1;
        }

        final String path = this.projectPath.toString();
        final String name = this.projectPath.getFileName().toString();

        // Process project
        LOGGER.info("Processing project: " + name);
        MavenLauncher launcher = getMavenLauncher(path, name);
        LOGGER.info("POM found at: " + launcher.getPomFile().getPath());

        // Build Spoon model
        CtModel model = buildSpoonModel(launcher);
        LOGGER.info("Total number of methods: " + countMethods(model));

        // Apply processor to model
        applyProcessor(model);

        // Save model in spooned/
        launcher.prettyprint();

        return 0;
    }

    private void applyProcessor(final CtModel model) {
        FirstMethodProcessor firstMethodProcessor = new FirstMethodProcessor();
        MethodProcessor methodProcessor = new MethodProcessor();
        model.processWith(firstMethodProcessor);
        model.processWith(methodProcessor);
        LOGGER.info("Modifiers present in project: " +
            methodProcessor.getAllMethodModifiersInProject());
        LOGGER.info("Number of candidate methods to check for purity: " +
            methodProcessor.getCandidateMethods().size());
        LOGGER.info("Candidate methods to check for purity: ");
            methodProcessor.getCandidateMethods().forEach(ctMethod -> System.out.println(ctMethod.getSignature()));
    }

    private CtModel buildSpoonModel(final MavenLauncher launcher) {
        launcher.buildModel();
        return launcher.getModel();
    }

    private MavenLauncher getMavenLauncher(final String projectPath, final String projectName) {
        MavenLauncher launcher = new MavenLauncher(projectPath, MavenLauncher.SOURCE_TYPE.APP_SOURCE);
        launcher.getEnvironment().setAutoImports(true);
        launcher.getEnvironment().setCommentEnabled(true);
        return launcher;
    }

    private int countMethods(final CtModel model) {
        int numberOfMethodsInProject = 0;
        for (CtType<?> s : model.getAllTypes()) numberOfMethodsInProject += s.getMethods().size();
        return numberOfMethodsInProject;
    }

    public Path getProjectPath() {
        return projectPath;
    }
}
