import org.hyperskill.hstest.dynamic.output.InfiniteLoopDetector;
import org.hyperskill.hstest.stage.StageTest;
import org.hyperskill.hstest.testcase.CheckResult;
import org.hyperskill.hstest.testcase.SimpleTestCase;
import org.hyperskill.hstest.testcase.TestCase;
import org.hyperskill.hstest.testing.TestedProgram;
import org.hyperskill.hstest.testing.execution.MainMethodExecutor;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class SharedBillsSplitterTestStage5 extends StageTest {

    public static final String FILTERED_BALANCE_FEEDBACK = "Program should output balance result for persons who contains if filter. However owes values should be the same as if balance were unfiltered.";
    private static final String UNKNOWN_COMMAND = "Unknown command";
    private static final String EXIT_ERROR = "Your program should stop after exit command";
    private static final String HELP_ERROR = "Help command should print all commands line by line in natural order";
    private static final String ILLEGAL_COMMAND_ARGUMENTS = "Illegal command arguments";
    private static final String ILLEGAL_ARGUMENTS_ERROR = "Your program should handle exceptions in incorrect command arguments input";
    private static final String UNKNOWN_GROUP = "Unknown group";
    private static final String NO_REPAYMENTS = "No repayments";
    private static final String WRONG_CALCULATIONS = "Wrong calculations. Program should output owes list that " +
            "if every person in this list repay his owes then everyone will have zero balance and everyone will be paid off";
    private static final String GIFT_TO = " gift to ";
    private static final String GROUP_PERSONS_FEEDBACK = "Persons in group should be printed line by line sorted in ascending order";
    private static final String BALANCE_OWES_FEEDBACK = "Owes should be sorted by Person who owes and Person whom owes";
    private static final String WRITE_OFF_FEEDBACK = "WriteOff should remove from storage all money operations till command date";
    private static final String INCORRECT_BALANCE = "The balance your program outputs is incorrect";
    private static final String INCORRECT_MONEY_FORMAT = "Money should be formatted with proper scale";
    private static final String EMPTY_GROUP = "Your program should output \"Group is empty\" if the purchase or show command is invoked on an empty group.";

    static {
        InfiniteLoopDetector.setWorking(false);
    }

    private final String databasePath;

    public SharedBillsSplitterTestStage5() {

        databasePath = "../testDB" + ".mv.db";
    }

    private static boolean equalsByLines(String sample, String linesStr) {

        List<String> sampleLines = strToLinesTrimmed(sample);
        List<String> lines = strToLinesTrimmed(linesStr);
        return sampleLines.equals(lines);
    }

    private static List<String> strToLinesTrimmed(String sample) {

        return sample.lines().map(String::trim).collect(Collectors.toList());
    }

    @Before
    public void doSomeBefore() {

        if (databasePath != null) {
            replaceDatabase();
        }
    }

    @After
    public void doSomeAfter() {

        if (databasePath != null) {
            revertDatabase();
        }
    }

    private void replaceDatabase() {

        String dbFilePath = System.getProperty("user.dir")
                + File.separator + databasePath;

        String dbTempFilePath = dbFilePath + "-real";

        Path dbFile = Paths.get(dbFilePath);
        Path dbTempFile = Paths.get(dbTempFilePath);

        try {
            if (dbTempFile.toFile().exists()) {
                Files.deleteIfExists(dbFile);
            } else if (dbFile.toFile().exists() && !dbTempFile.toFile().exists()) {
                Files.move(dbFile, dbTempFile);
            }
        } catch (IOException ignored) {
        }
    }

    private void revertDatabase() {

        String dbFilePath = System.getProperty("user.dir")
                + File.separator + databasePath;

        String dbTempFilePath = dbFilePath + "-real";

        Path dbFile = Paths.get(dbFilePath);
        Path dbTempFile = Paths.get(dbTempFilePath);

        try {
            Files.deleteIfExists(dbFile);
            if (dbTempFile.toFile().isFile()) {
                Files.move(dbTempFile, dbFile);
            }
        } catch (IOException ignored) {
        }
    }

    private <T> TestCase<T> caseToDynamicTesting(TestCase<T> testCase) {

        String feedback = testCase.getFeedback();
        String input = testCase.getInput();
        T attach = testCase.getAttach();
        BiFunction<String, T, CheckResult> checkFunc = testCase.getCheckFunc();

        return new TestCase<T>()
                .setFeedback(feedback)
                .setDynamicTesting(() -> {
                    TestedProgram p = new TestedProgram();
                    ((MainMethodExecutor) p.getProgramExecutor()).setUseSeparateClassLoader(false);
                    p.setReturnOutputAfterExecution(false);
                    p.start();
                    p.execute(input);
                    return checkFunc.apply(p.getOutput(), attach);
                });
    }

    @Override
    public List<TestCase> generate() {

        return List.of(

                caseToDynamicTesting(new TestCase<String>()
                                             .setCheckFunc(this::checkUnknownCommand)
                                             .setAttach("someAttachText")
                                             .setInput("someRandomText\n" +
                                                               "exit")),

                caseToDynamicTesting(new SimpleTestCase("repay Ann\n" +
                                                        "exit",
                                                        ILLEGAL_COMMAND_ARGUMENTS)
                                             .setFeedback(ILLEGAL_ARGUMENTS_ERROR)),

                caseToDynamicTesting(new TestCase<String>()
                                             .setCheckFunc(this::checkHelpCommand)
                                             .setInput(concatLines(Commands.help.toString(), Commands.exit.toString()))),

                new TestCase<String>().setDynamicTesting(() -> {
                    TestedProgram main = new TestedProgram();
                    ((MainMethodExecutor) main.getProgramExecutor()).setUseSeparateClassLoader(false);
                    main.start();
                    main.execute(Commands.exit.toString());
                    if (!main.isFinished()) {
                        return CheckResult.wrong(EXIT_ERROR);
                    }
                    return CheckResult.correct();
                }),

                new TestCase<String>().setDynamicTesting(() -> {
                    TestedProgram main = new TestedProgram();
                    ((MainMethodExecutor) main.getProgramExecutor()).setUseSeparateClassLoader(false);
                    main.start();
                    main.execute("borrow Ann Bob 1.00");
                    final String FUTURE_DATE = "3030.03.30";
                    main.execute(FUTURE_DATE + " purchase Bob coffee 3.50 (Bob, Ann)");
                    main.execute("writeOff");
                    { //1st
                        String result = main.execute("balance close");
                        if (!result.startsWith(NO_REPAYMENTS)) {
                            return CheckResult.wrong(WRITE_OFF_FEEDBACK);
                        }
                    }
                    { //2nd
                        main.execute(FUTURE_DATE + " writeOff");
                        String result = main.execute(FUTURE_DATE + " balance close");
                        if (!result.startsWith(NO_REPAYMENTS)) {
                            return CheckResult.wrong(WRITE_OFF_FEEDBACK);
                        }
                    }
                    main.execute("exit");
                    return CheckResult.correct();
                }),

                new TestCase<String>().setDynamicTesting(() -> {
                    {
                        TestedProgram main = new TestedProgram();
                        ((MainMethodExecutor) main.getProgramExecutor()).setUseSeparateClassLoader(false);
                        main.start();
                        main.execute("writeOff");
                        main.execute("group create TEAM (Diana, Elon, Bob, Ann)");
                        main.execute("purchase Elon icecream 4.80 (TEAM)");
                        main.execute("borrow Ann Bob 1.05");
                        main.execute("repay Ann Bob 5.01");
                        main.execute("exit");
                    }
                    {
                        TestedProgram main = new TestedProgram();
                        ((MainMethodExecutor) main.getProgramExecutor()).setUseSeparateClassLoader(false);
                        main.start();
                        String groupResult = main.execute("group show TEAM");
                        if (!equalsByLines(groupResult, concatLines("Ann", "Bob", "Diana", "Elon"))) {
                            return CheckResult.wrong(GROUP_PERSONS_FEEDBACK + ". Also person and group should be stored in database");
                        }
                        String balanceResult = main.execute("balance close");
                        if (!equalsByLines(balanceResult, """
                            Ann owes Elon 1.20
                            Bob owes Ann 3.96
                            Bob owes Elon 1.20
                            Diana owes Elon 1.20""")) {
                            return CheckResult.wrong(BALANCE_OWES_FEEDBACK + " Also all payment operations should be stored in database");
                        }
                        main.execute("exit");
                    }
                    return CheckResult.correct();
                }),

                new TestCase<String>().setDynamicTesting(() -> {
                    {
                        TestedProgram main = new TestedProgram();
                        ((MainMethodExecutor) main.getProgramExecutor()).setUseSeparateClassLoader(false);
                        main.start();
                        String output;
                        String[] words;

                        main.execute("writeOff");
                        main.execute("2020.09.30 borrow Ann Bob 20.10");
                        main.execute("2020.10.01 repay Ann Bob 10.22");
                        main.execute("2020.10.10 borrow Bob Ann 7.35");
                        main.execute("2020.10.15 repay Ann Bob 8.99");
                        main.execute("repay Bob Ann 6.46");
                        output = main.execute("2020.09.25 balance").trim();
                        if (!output.equals(NO_REPAYMENTS)) {
                            return CheckResult.wrong(INCORRECT_BALANCE);
                        }

                        output = main.execute("2020.10.30 balance open").trim();
                        words = output.split("\\s");
                        if (!isMoneyFormatted(words[words.length - 1])) {
                            return CheckResult.wrong(INCORRECT_MONEY_FORMAT);
                        }
                        if (!output.equals("Ann owes Bob 20.10")) {
                            return CheckResult.wrong(INCORRECT_BALANCE);
                        }

                        output = main.execute("2020.10.20 balance close").trim();
                        words = output.split("\\s");
                        if (!isMoneyFormatted(words[words.length - 1])) {
                            return CheckResult.wrong(INCORRECT_MONEY_FORMAT);
                        }
                        if (!output.equals("Bob owes Ann 6.46")) {
                            return CheckResult.wrong(INCORRECT_BALANCE);
                        }

                        output = main.execute("balance close").trim();
                        if (!output.equals(NO_REPAYMENTS)) {
                            return CheckResult.wrong(INCORRECT_BALANCE);
                        }

                        main.execute("exit");


                    }
                    return CheckResult.correct();
                }),

                new TestCase<String>().setDynamicTesting(() -> {
                    {
                        String[] response = {
                            "Ann owes Bob 14.00",
                            "Chuck owes Bob 7.00",
                            "Diana owes Bob 5.00",
                            "Diana owes Chuck 26.00",
                            "Elon owes Diana 12.00"
                        };

                        TestedProgram main = new TestedProgram();
                        ((MainMethodExecutor) main.getProgramExecutor()).setUseSeparateClassLoader(false);
                        String output;
                        String[] lines;
                        main.start();

                        main.execute("writeOff");
                        main.execute("borrow Ann Bob 25");
                        main.execute("repay Ann Bob 15");
                        main.execute("repay Bob Chuck 7");
                        main.execute("borrow Ann Bob 4");
                        main.execute("repay Bob Diana 5");
                        main.execute("borrow Elon Diana 12");
                        main.execute("repay Chuck Diana 14");
                        main.execute("repay Chuck Diana 12");
                        output = main.execute("balance close").trim();
                        lines = output.split("\n");

                        if (lines.length < response.length) {
                            return CheckResult.wrong(INCORRECT_BALANCE);
                        }

                        for (int i = 0; i < response.length; i++) {
                            if (!lines[i].equals(response[i]) && lines[i].equals(NO_REPAYMENTS)) {
                                return CheckResult.wrong(INCORRECT_BALANCE);
                            } else if (!lines[i].equals(response[i])) {
                                return CheckResult.wrong("Owes should be sorted by Person who owes and Person whom owes");
                            }
                        }

                        if (!isSortedInNaturalOrder(lines)) {
                            return CheckResult.wrong("List of repayments should be sorted in a natural order.");
                        }

                        for (String line : lines) {
                            String[] words = line.split("\\s");
                            if (!isMoneyFormatted(words[words.length - 1])) {
                                return CheckResult.wrong(INCORRECT_MONEY_FORMAT);
                            }
                        }

                        main.execute("exit");
                    }

                    return CheckResult.correct();
                }),

                new TestCase<String>().setDynamicTesting(() -> {
                    Random random = new Random();
                    List<String> persons = List.of("Annabelle", "Billibob", "Carlos", "Diana", "Elon", "Finny");
                    String keyPerson = persons.get(random.nextInt(persons.size()));
                    BigDecimal keyBalanceBorrow = BigDecimal.ZERO;
                    BigDecimal keyBalanceRepay = BigDecimal.ZERO;
                    TestedProgram main = new TestedProgram();
                    ((MainMethodExecutor) main.getProgramExecutor()).setUseSeparateClassLoader(false);
                    main.start();
                    main.execute("writeOff");
                    for (int i = 0; i < 100; i++) {
                        String personFrom = persons.get(random.nextInt(persons.size()));
                        String personTo = persons.get(random.nextInt(persons.size()));
                        if (personFrom.equalsIgnoreCase(personTo)) {
                            continue;
                        }
                        Commands command;
                        BigDecimal amount = new BigDecimal(String.format("%d.%d", random.nextInt(200), random.nextInt(99)));
                        if (random.nextBoolean()) {
                            command = Commands.borrow;
                            if (personFrom.equals(keyPerson)) {
                                keyBalanceBorrow = keyBalanceBorrow.add(amount);
                            }
                            if (personTo.equals(keyPerson)) {
                                keyBalanceBorrow = keyBalanceBorrow.subtract(amount);
                            }
                        } else {
                            command = Commands.repay;
                            if (personFrom.equals(keyPerson)) {
                                keyBalanceRepay = keyBalanceRepay.add(amount);
                            }
                            if (personTo.equals(keyPerson)) {
                                keyBalanceRepay = keyBalanceRepay.subtract(amount);
                            }
                        }
                        String line = String.format("%s %s %s %s", command, personFrom, personTo, amount);
                        main.execute(line);
                    }
                    String result = main.execute("balance close");
                    Optional<BigDecimal> sum = Arrays.stream(result.split("\n"))
                                                     .filter(it -> it.contains(keyPerson))
                                                     .map(it -> {
                                                         String[] split = it.split("\\s+");
                                                         Character sign = it.startsWith(keyPerson) ? '+' : '-';
                                                         return sign + split[split.length - 1];
                                                     })
                                                     .map(BigDecimal::new)
                                                     .reduce(BigDecimal::add);

                    BigDecimal sumBalance = keyBalanceBorrow.subtract(keyBalanceRepay);
                    if (sumBalance.compareTo(sum.orElse(BigDecimal.ZERO)) == 0) {
                        main.execute("exit");
                        return CheckResult.correct();
                    }
                    return CheckResult.wrong(WRONG_CALCULATIONS);
                }),


                new TestCase<String>().setDynamicTesting(() -> {
                    TestedProgram main = new TestedProgram();
                    ((MainMethodExecutor) main.getProgramExecutor()).setUseSeparateClassLoader(false);
                    main.start();
                    if (!main.execute("group create lowerCaseText").contains(ILLEGAL_COMMAND_ARGUMENTS)) {
                        return CheckResult.wrong(String.format(
                                "Group name must be UPPERCASE, otherwise \"%s\" should be printed",
                                ILLEGAL_COMMAND_ARGUMENTS));
                    }
                    if (!main.execute("group show NOTFOUNDGROUP").contains(UNKNOWN_GROUP)) {
                        return CheckResult.wrong(
                                String.format("It should be printed \"%s\" if the group have not been created yet", UNKNOWN_GROUP));
                    }

                    main.execute("group create BOYS (Elon, Bob, Chuck)");
                    String showGroupResult = main.execute("group show BOYS").trim();
                    if (!equalsByLines(showGroupResult, """
                        Bob
                        Chuck
                        Elon""")) {
                        return CheckResult.wrong(GROUP_PERSONS_FEEDBACK);
                    }
                    main.execute("exit");
                    return CheckResult.correct();
                }),

                new TestCase<String>().setDynamicTesting(() -> {
                    TestedProgram main = new TestedProgram();
                    ((MainMethodExecutor) main.getProgramExecutor()).setUseSeparateClassLoader(false);
                    main.start();
                    main.execute("writeOff");
                    main.execute("group create COFFEETEAM (Ann, Bob)");
                    main.execute("purchase Bob coffee 10 (COFFEETEAM)");
                    String balanceFirst = main.execute("balance close").trim();
                    if (balanceFirst.contains("Bob owes")) {
                        return CheckResult.wrong("Only Ann owes Bob. Bob should not owe to himself");
                    }
                    if (!balanceFirst.equals("Ann owes Bob 5.00")) {
                        return CheckResult.wrong(INCORRECT_BALANCE);
                    }
                    main.execute("repay Ann Bob 5.00");
                    String balanceSecond = main.execute("balance close").trim();
                    if (!balanceSecond.equals(NO_REPAYMENTS)) {
                        return CheckResult.wrong(
                                "If everybody owes zero, it should be printed \"No repayments\"");
                    }
                    main.execute("exit");
                    return CheckResult.correct();
                }),

                new TestCase<String>().setDynamicTesting(() -> {
                    TestedProgram main = new TestedProgram();
                    ((MainMethodExecutor) main.getProgramExecutor()).setUseSeparateClassLoader(false);
                    main.start();
                    main.execute("writeOff");
                    main.execute("group create FRIENDS (Ann, Bob, Chuck)");
                    main.execute("purchase Elon chocolate 12.50 (FRIENDS)");
                    String balanceResult = main.execute("balance close");
                    if (!equalsByLines(balanceResult, """
                        Ann owes Elon 4.17
                        Bob owes Elon 4.17
                        Chuck owes Elon 4.16""")) {
                        return CheckResult.wrong("The remainder after division should be spread amongst the first N persons sorted in natural order just like the examples");
                    }
                    main.execute("exit");
                    return CheckResult.correct();
                }),

                new TestCase<String>().setDynamicTesting(() -> {
                    TestedProgram main = new TestedProgram();
                    ((MainMethodExecutor) main.getProgramExecutor()).setUseSeparateClassLoader(false);
                    main.start();
                    main.execute("writeOff");
                    main.execute("group create BOYS (Elon, Bob, Chuck)");
                    main.execute("group create GIRLS (Ann, Diana)");
                    main.execute("2020.10.20 purchase Diana flowers 15.65 (BOYS)");
                    main.execute("2020.10.21 purchase Chuck chocolate 6.30 (BOYS)");
                    main.execute("2020.10.22 purchase Bob icecream 3.99 (GIRLS)");
                    String balanceCloseResult = main.execute("balance close");
                    if (!equalsByLines(balanceCloseResult, """
                        Ann owes Bob 2.00
                        Bob owes Chuck 2.10
                        Bob owes Diana 3.23
                        Chuck owes Diana 5.22
                        Elon owes Chuck 2.10
                        Elon owes Diana 5.21""")) {
                        return CheckResult.wrong("Output should be the same as in example");
                    }

                    main.execute("exit");
                    return CheckResult.correct();
                }),

                new TestCase<String>().setDynamicTesting(() -> {
                    TestedProgram main = new TestedProgram();
                    ((MainMethodExecutor) main.getProgramExecutor()).setUseSeparateClassLoader(false);
                    main.start();
                    main.execute("group create GIRLS (Ann, Diana)");
                    main.execute("group create TEAM (+Bob, GIRLS, -Frank, Chuck)");
                    String groupResult = main.execute("group show TEAM");
                    if (!equalsByLines(groupResult, """
                        Ann
                        Bob
                        Chuck
                        Diana""")) {
                        return CheckResult.wrong("Program should include Bob, Chuck and persons from GIRLS, also Frank should be excluded");
                    }

                    main.execute("exit");
                    return CheckResult.correct();
                }),

                new TestCase<String>().setDynamicTesting(() -> {
                    TestedProgram main = new TestedProgram();
                    ((MainMethodExecutor) main.getProgramExecutor()).setUseSeparateClassLoader(false);
                    main.start();
                    main.execute("writeOff");
                    main.execute("group create GIRLS (Ann, Diana)");
                    main.execute("group create TEAM (+Bob, GIRLS, -Frank, Chuck)");
                    main.execute("2020.10.20 purchase Diana flowers 15.65 (TEAM, Elon, -GIRLS)");
                    main.execute("2020.10.21 purchase Elon ChuckBirthdayGift 20.99 (TEAM, -Chuck)");
                    String balanceResult = main.execute("balance close").trim();

                    if (!isSortedInNaturalOrder(balanceResult.split("\n"))) {
                        return CheckResult.wrong("List of repayments should be sorted in a natural order.");
                    }

                    if (balanceResult.contains("Ann owes Diana")) {
                        return CheckResult.wrong("Program should split flowers bill on TEAM with Elon without GIRLS");
                    }

                    if (balanceResult.contains("Chuck owes Elon")) {
                        return CheckResult.wrong("Program should split ChuckBirthdayGift bill on TEAM without Chuck");
                    }

                    if (balanceResult.contains("Elon owes Diana")) {
                        return CheckResult.wrong("Wrong calculations. Elon initially owes Diana for " +
                                                         "the flower purchase but remember Elon purchases " +
                                                         "ChuckBirthdayGift which covers the cost, plus extra");
                    }

                    if (!equalsByLines(balanceResult, """
                        Ann owes Elon 7.00
                        Bob owes Diana 5.22
                        Bob owes Elon 7.00
                        Chuck owes Diana 5.22
                        Diana owes Elon 1.78""")) {
                        return CheckResult.wrong(INCORRECT_BALANCE);
                    }

                    main.execute("exit");
                    return CheckResult.correct();
                }),

                new TestCase<String>().setDynamicTesting(() -> {
                    TestedProgram main = new TestedProgram();
                    ((MainMethodExecutor) main.getProgramExecutor()).setUseSeparateClassLoader(false);
                    String output;
                    main.start();
                    main.execute("writeOff");
                    main.execute("group create SOMEGROUP (Bob, -Bob)");
                    output = main.execute("group show SOMEGROUP").toLowerCase();
                    if (!output.contains("empty")) {
                        return CheckResult.wrong(EMPTY_GROUP);
                    }

                    main.execute("group add SOMEGROUP (Bob)");
                    main.execute("group remove SOMEGROUP (Bob)");
                    output = main.execute("group show SOMEGROUP").toLowerCase();
                    if (!output.contains("empty")) {
                        return CheckResult.wrong(EMPTY_GROUP);
                    }

                    main.execute("group create ANOTHERGROUP (SOMEGROUP)");
                    output = main.execute("group show ANOTHERGROUP").toLowerCase();
                    if (!output.contains("empty")) {
                        return CheckResult.wrong(EMPTY_GROUP);
                    }

                    output = main.execute("2020.10.21 purchase Elon chocolate 6.30 (SOMEGROUP)").toLowerCase();
                    if (!output.contains("group is empty")) {
                        return CheckResult.wrong(EMPTY_GROUP);
                    }

                    return CheckResult.correct();
                }),

                new TestCase<String>().setDynamicTesting(() -> {
                    TestedProgram main = new TestedProgram();
                    ((MainMethodExecutor) main.getProgramExecutor()).setUseSeparateClassLoader(false);
                    main.start();
                    main.execute("group create SOMEGROUP (Bob)");
                    main.execute("group create GIRLS (Ann, Diana)");
                    main.execute("group create BOYS (Bob, Chuck, Elon)");
                    main.execute("group add SOMEGROUP (GIRLS, Frank)");
                    main.execute("group remove SOMEGROUP (-BOYS, Bob, +Frank)");
                    String groupResult = main.execute("group show SOMEGROUP");
                    if (!equalsByLines(groupResult, """
                        Ann
                        Bob
                        Diana""")) {
                        return CheckResult.wrong("First of all program should collect persons from brackets:" +
                             "At first collect all additions, and then remove all persons to delete." +
                             "eg. group <some group command> GROUP (-BOYS, Bob, +Frank): " +
                             "program should collect Bob and Frank" +
                             "and then remove all persons from BOYS");
                    }
                    main.execute("exit");
                    return CheckResult.correct();
                }),

                new TestCase<String>().setDynamicTesting(() -> {
                    List<String> persons = List.of("Gordon", "Bob", "Ann", "Chuck", "Elon", "Diana", "Foxy");

                    AtomicInteger counter = new AtomicInteger(0);
                    Map<String, Integer> map = persons.stream().sorted()
                                                      .collect(Collectors.toMap(it -> it, it -> counter.getAndIncrement(),
                                                                                (a, b) -> {
                                                                                    throw new UnsupportedOperationException();
                                                                                },
                                                                                LinkedHashMap::new));

                    TestedProgram main = new TestedProgram();
                    ((MainMethodExecutor) main.getProgramExecutor()).setUseSeparateClassLoader(false);
                    main.start();
                    main.execute(String.format("group create SOMESANTAGROUP (%s)", String.join(",", persons)));
                    String secretSantaResult = main.execute("secretSanta SOMESANTAGROUP");

                    if (!secretSantaResult.lines().allMatch(it -> it.contains(GIFT_TO))) {
                        return CheckResult.wrong("Each line should contains \" gift to \"");
                    }
                    List<Integer> sendersList = new ArrayList<>();
                    List<Integer> receiversList = new ArrayList<>();
                    secretSantaResult.lines().map(String::trim)
                                     .map(it -> it.split(GIFT_TO))
                                     .forEach(it -> {
                                         sendersList.add(map.get(it[0]));
                                         receiversList.add(map.get(it[1]));
                                     });
                    if (sendersList.size() != persons.size() || !isSorted(sendersList)) {
                        return CheckResult.wrong("Program should print persons who will gift to someone in ascending order");
                    }
                    for (int i = 0; i < sendersList.size(); i++) {
                        if (sendersList.get(i).equals(receiversList.get(i)) && sendersList.size() > 1) {
                            return CheckResult.wrong("Person should not gift a present to himself (in groups larger than 1)");
                        }
                        Integer receiverId = receiversList.get(i);
                        if (sendersList.get(receiverId) == i && sendersList.size() > 2) {
                            return CheckResult.wrong("Person should not gift and receive a present from the same other person (in groups larger than 2)");
                        }
                    }
                    main.execute("exit");
                    return CheckResult.correct();
                }),

                new TestCase<String>().setDynamicTesting(() -> {
                    TestedProgram main = new TestedProgram();
                    ((MainMethodExecutor) main.getProgramExecutor()).setUseSeparateClassLoader(false);
                    main.start();
                    main.execute("writeOff");
                    main.execute("group create TEAM (Bob, Ann, Frank, Chuck, Elon, Diana)");
                    main.execute("2020.12.25 cashBack YourCompany secretSantaGift 24.00 (TEAM)");
                    String balanceResult = main.execute("2020.12.25 balance close");
                    if (!equalsByLines(balanceResult, """
                        YourCompany owes Ann 4.00
                        YourCompany owes Bob 4.00
                        YourCompany owes Chuck 4.00
                        YourCompany owes Diana 4.00
                        YourCompany owes Elon 4.00
                        YourCompany owes Frank 4.00""")) {
                        return CheckResult.wrong("Program should output list of YourCompany owes to everyone in TEAM");
                    }
                    main.execute("exit");
                    return CheckResult.correct();
                }),

                new TestCase<String>().setDynamicTesting(() -> {
                    TestedProgram main = new TestedProgram();
                    ((MainMethodExecutor) main.getProgramExecutor()).setUseSeparateClassLoader(false);
                    main.start();
                    strToLinesTrimmed("""
                        writeOff
                        group create TEAM (Ann, Bob, Chuck, Diana, Elon, Frank)
                        group create CAR (Diana, Elon)
                        group create BUS (Ann, Bob, Chuck, Frank)
                        purchase Chuck busTickets 5.25 (BUS, -Frank)
                        purchase Elon fuel 25 (CAR, Frank)
                        purchase Ann chocolate 2.99 (BUS, -Bob, CAR)
                        purchase Diana soda 5.45 (TEAM, -Ann, -Chuck)
                        purchase Frank bbq 29.90 (TEAM, CAR, BUS, -Frank, -Bob)
                        cashBack YourCompany party 12 (TEAM, BUS)
                        cashBack YourCompany tickets 3.50 (BUS)
                        borrow Frank Bob 10
                        repay Chuck Diana 20""")
                            .forEach(main::execute);
                    String balanceResult = main.execute("balance close");
                    if (!equalsByLines(balanceResult, """
                        Ann owes Chuck 1.15
                        Ann owes Frank 6.89
                        Bob owes Chuck 1.75
                        Bob owes Diana 1.37
                        Chuck owes Frank 7.48
                        Diana owes Ann 0.60
                        Diana owes Chuck 20.00
                        Diana owes Elon 6.98
                        Diana owes Frank 6.11
                        Elon owes Ann 0.60
                        Frank owes Bob 10.00
                        Frank owes Elon 0.86
                        YourCompany owes Ann 2.88
                        YourCompany owes Bob 2.88
                        YourCompany owes Chuck 2.87
                        YourCompany owes Diana 2.00
                        YourCompany owes Elon 2.00
                        YourCompany owes Frank 2.87""")) {
                        return CheckResult.wrong(WRONG_CALCULATIONS);
                    }
                    main.execute("exit");
                    return CheckResult.correct();
                }),

                new TestCase<String>().setDynamicTesting(() -> {
                    TestedProgram main = new TestedProgram();
                    ((MainMethodExecutor) main.getProgramExecutor()).setUseSeparateClassLoader(false);
                    main.start();
                    main.execute("writeOff");
                    main.execute("group create BOBTEAM (Frank, Bob)");
                    main.execute("group create AGROUP (Bob, -Bob)");
                    main.execute("purchase Ann coffee 12.00 (Chuck, Ann, Bob)");
                    {
                        String balanceResult = main.execute("balance close (Bob, Ann)");
                        if (!equalsByLines(balanceResult, "Bob owes Ann 4.00")) {
                            return CheckResult.wrong(FILTERED_BALANCE_FEEDBACK);
                        }
                    }
                    {
                        String balanceResult = main.execute("balance close (-Bob, BOBTEAM)");
                        if (!equalsByLines(balanceResult, NO_REPAYMENTS)) {
                            return CheckResult.wrong(String.format("Program should output \"%s\" if no one person in filter have owes", NO_REPAYMENTS));
                        }
                    }
                    {
                        String balanceResult = main.execute("balance close (AGROUP)").trim().toLowerCase();
                        if (!balanceResult.contains("group is empty")) {
                            return CheckResult.wrong("Program should output \"Group is empty\" if the balance command is entered on an empty group");
                        }
                    }
                    {
                        String balanceResult = main.execute("balance close (GROUPIDIDNOTCREATE)").trim().toLowerCase();
                        if (!balanceResult.contains("not exist")) {
                            return CheckResult.wrong("Program should output \"Group does not exist\" if the balance command is entered on a group that has not been created.");
                        }
                    }
                    main.execute("exit");
                    return CheckResult.correct();
                })


        );
    }

    private boolean isMoneyFormatted(String s) {

        return s.matches("\\d+.\\d+");
    }

    private boolean isSortedInNaturalOrder(String[] arr) {

        String previous = "";

        for (String current : arr) {
            if (current.compareTo(previous) < 0) {
                return false;
            }
            previous = current;
        }
        return true;
    }

    private <T extends Comparable<T>> boolean isSorted(List<T> list) {

        ArrayList<T> sorted = new ArrayList<>(list);
        return sorted.equals(list);
    }

    private CheckResult checkHelpCommand(String reply, String attach) {

        String[] replyArr = reply.split("\n");
        List<String> commandList = getCommandList();
        if (replyArr.length != commandList.size()) {
            return CheckResult.wrong(HELP_ERROR);
        }
        for (int i = 0; i < replyArr.length; i++) {
            if (!replyArr[i].toLowerCase().startsWith(commandList.get(i).toLowerCase())) {
                return CheckResult.wrong(HELP_ERROR);
            }
        }
        return CheckResult.correct();
    }

    private CheckResult checkUnknownCommand(String reply, String attach) {

        try {
            reply = reply.trim();
            Commands command = Commands.valueOf(reply);
        } catch (IllegalArgumentException e) {
            if (!reply.toLowerCase().startsWith(UNKNOWN_COMMAND.toLowerCase())) {
                return CheckResult.wrong(String.format("For unknown command output should starts with: %s", UNKNOWN_COMMAND));
            }
        }
        return CheckResult.correct();
    }

    private List<String> getCommandList() {

        return Arrays.stream(Commands.values())
                     .map(Enum::toString)
                     .sorted().collect(Collectors.toList());
    }

    private String concatLines(List<String> strings) {

        return String.join("\n", strings);
    }

    private String concatLines(String... strings) {

        return String.join("\n", strings);
    }

    enum Commands {
        help,
        borrow,
        repay,
        balance,
        exit,
        group,
        purchase,
        secretSanta,
        cashBack,
        writeOff
    }
}
