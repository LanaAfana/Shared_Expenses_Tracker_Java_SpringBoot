import org.hyperskill.hstest.dynamic.output.InfiniteLoopDetector;
import org.hyperskill.hstest.stage.StageTest;
import org.hyperskill.hstest.testcase.CheckResult;
import org.hyperskill.hstest.testcase.SimpleTestCase;
import org.hyperskill.hstest.testcase.TestCase;
import org.hyperskill.hstest.testing.TestedProgram;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SharedBillsSplitterTestStage3 extends StageTest {

    private static final String UNKNOWN_COMMAND = "Unknown command";
    private static final String EXIT_ERROR = "Your program should stop after exit command";
    private static final String HELP_ERROR = "Help command should print all commands line by line in natural order";
    private static final String ILLEGAL_COMMAND_ARGUMENTS = "Illegal command arguments";
    private static final String ILLEGAL_ARGUMENTS_ERROR = "Your program should handle exceptions in incorrect command arguments input";
    private static final String UNKNOWN_GROUP = "Unknown group";
    private static final String NO_REPAYMENTS = "No repayments";
    private static final String WRONG_CALCULATIONS = "Wrong calculations. Program should output owes list that " +
            "if every person in this list repay his owes then everyone will have zero balance and everyone will be paid off";
    private static final String INCORRECT_BALANCE = "The balance your program outputs is incorrect";
    private static final String INCORRECT_MONEY_FORMAT = "Money should be formatted with proper scale";
    private static final String EMPTY_GROUP = "Your program should output \"Group is empty\" if the purchase or show command is invoked on an empty group.";

    static {
        InfiniteLoopDetector.setWorking(false);
    }

    private static boolean equalsByLines(String sample, String linesStr) {

        List<String> sampleLines = strToLinesTrimmed(sample);
        List<String> lines = strToLinesTrimmed(linesStr);
        return sampleLines.equals(lines);
    }

    private static List<String> strToLinesTrimmed(String sample) {

        return sample.lines().map(String::trim).collect(Collectors.toList());
    }

    @Override
    public List<TestCase> generate() {

        return List.of(
                new TestCase<String>()
                        .setCheckFunc(this::checkUnknownCommand)
                        .setAttach("someAttachText")
                        .setInput("someRandomText\n" +
                                  "exit"),

                new SimpleTestCase("repay Ann\n" +
                                   "exit",
                                   ILLEGAL_COMMAND_ARGUMENTS)
                        .setFeedback(ILLEGAL_ARGUMENTS_ERROR),

                new TestCase<String>()
                        .setCheckFunc(this::checkHelpCommand)
                        .setInput(concatLines(Commands.help.toString(), Commands.exit.toString())),

                new TestCase<String>().setDynamicTesting(() -> {
                    TestedProgram main = new TestedProgram();
                    main.start();
                    main.execute(Commands.exit.toString());
                    if (!main.isFinished()) {
                        return CheckResult.wrong(EXIT_ERROR);
                    }
                    return CheckResult.correct();
                }),

                new TestCase<String>().setDynamicTesting(() -> {
                    TestedProgram main = new TestedProgram();
                    main.start();
                    String output;
                    String[] words;

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

                    return CheckResult.correct();
                }),

                new TestCase<String>().setDynamicTesting(() -> {
                    String[] response = {
                            "Ann owes Bob 14.00",
                            "Chuck owes Bob 7.00",
                            "Diana owes Bob 5.00",
                            "Diana owes Chuck 26.00",
                            "Elon owes Diana 12.00"
                    };
                    TestedProgram main = new TestedProgram();
                    String output;
                    String[] lines;
                    main.start();

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
                        }

                        String[] words = lines[i].split("\\s");
                        if (!isMoneyFormatted(words[words.length - 1])) {
                            return CheckResult.wrong(INCORRECT_MONEY_FORMAT);
                        }

                        if (!lines[i].equals(response[i])) {
                            return CheckResult.wrong(
                                    "Owes should be sorted by Person who owes and Person whom owes"
                            );
                        }
                    }

                    if (!isSortedInNaturalOrder(lines)) {
                        return CheckResult.wrong("List of repayments should be sorted in a natural order.");
                    }

                    main.execute("exit");

                    return CheckResult.correct();
                }),

                new TestCase<String>().setDynamicTesting(() -> {
                    Random random = new Random();
                    List<String> persons = List.of("Annabelle", "Billibob", "Carlos", "Diana", "Elon", "Finny");
                    String keyPerson = persons.get(random.nextInt(persons.size()));
                    BigDecimal keyBalanceBorrow = BigDecimal.ZERO;
                    BigDecimal keyBalanceRepay = BigDecimal.ZERO;
                    TestedProgram main = new TestedProgram();
                    main.start();
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
                        return CheckResult.correct();
                    }
                    return CheckResult.wrong(WRONG_CALCULATIONS);
                }),


                new TestCase<String>().setDynamicTesting(() -> {
                    TestedProgram main = new TestedProgram();
                    main.start();
                    if (!main.execute("group create lowerCaseText").contains(ILLEGAL_COMMAND_ARGUMENTS)) {
                        return CheckResult.wrong(String.format("Group name must be UPPERCASE, otherwise \"%s\" should be printed",
                                                               ILLEGAL_COMMAND_ARGUMENTS));
                    }
                    if (!main.execute("group show NOTFOUNDGROUP").contains(UNKNOWN_GROUP)) {
                        return CheckResult.wrong(String.format("\"%s\" should be printed if the group have not been created yet", UNKNOWN_GROUP));
                    }

                    main.execute("group create BOYS (Elon, Bob, Chuck)");
                    String showGroupResult = main.execute("group show BOYS").trim();
                    if (!equalsByLines(showGroupResult, """
                        Bob
                        Chuck
                        Elon""")) {
                        return CheckResult.wrong("Persons should be printed line by line sorted in ascending order");
                    }
                    return CheckResult.correct();
                }),

                new TestCase<String>().setDynamicTesting(() -> {
                    TestedProgram main = new TestedProgram();
                    main.start();
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
                        return CheckResult.wrong("If everybody owes zero, it should be printed \"No repayments\"");
                    }
                    return CheckResult.correct();
                }),

                new TestCase<String>().setDynamicTesting(() -> {
                    TestedProgram main = new TestedProgram();
                    main.start();
                    main.execute("group create FRIENDS (Ann, Bob, Chuck)");
                    main.execute("purchase Elon chocolate 12.50 (FRIENDS)");
                    String balanceResult = main.execute("balance close");
                    if (!equalsByLines(balanceResult, """
                        Ann owes Elon 4.17
                        Bob owes Elon 4.17
                        Chuck owes Elon 4.16""")) {
                        return CheckResult.wrong("The remainder after division should be spread amongst the first N persons sorted in natural order just like the examples");
                    }
                    return CheckResult.correct();
                }),

                new TestCase<String>().setDynamicTesting(() -> {
                    TestedProgram main = new TestedProgram();
                    main.start();
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
                    return CheckResult.correct();
                }),

                new TestCase<String>().setDynamicTesting(() -> {
                    TestedProgram main = new TestedProgram();
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
                    return CheckResult.correct();
                }),

                new TestCase<String>().setDynamicTesting(() -> {
                    TestedProgram main = new TestedProgram();
                    main.start();
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
                    return CheckResult.correct();
                }),

                new TestCase<String>().setDynamicTesting(() -> {
                    TestedProgram main = new TestedProgram();
                    String output;
                    main.start();
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
                        return CheckResult.wrong("First of all program should collect persons from brackets: " +
                            "At first collect all additions, and then remove all persons to delete." +
                            "eg. group <some group command> GROUP (-BOYS, Bob, +Frank): " +
                            "at first program should collect Bob and Frank " +
                            "and then remove all persons from BOYS");
                    }
                    return CheckResult.correct();
                })
        );
    }

    private boolean isMoneyFormatted(String s) {

        return s.matches("\\d+\\.\\d{2}");
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
        purchase
    }
}
