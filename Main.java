import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class Main {
    private static final Color CANVAS = new Color(10, 18, 31);
    private static final Color INK = new Color(232, 239, 248);
    private static final Color MUTED = new Color(143, 162, 187);
    private static final Color LINE = new Color(39, 57, 81);
    private static final Color ACCENT = new Color(34, 199, 165);
    private final JTextField display = new JTextField("0");
    private final DefaultListModel<String> history = new DefaultListModel<>();
    private Double firstOperand;
    private String pendingOperator;
    private boolean waitingForOperand;

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("gui")) {
            launchGui();
        } else {
            runCli();
        }
    }

    private static void launchGui() {
        SwingUtilities.invokeLater(() -> new Main().showWindow());
    }

    private static void runCli() {
        if (System.console() != null) {
            runInteractiveCli();
        } else {
            runLineCli();
        }
    }

    private static void runInteractiveCli() {
        List<String> recent = new ArrayList<>();
        List<String> inputHistory = new ArrayList<>();
        String status = "Ready. Type help for commands.";
        try {
            setRawMode(true);
            while (true) {
                String command = readEditableLine("command", recent, inputHistory, status);
                if (command == null || command.equalsIgnoreCase("exit")) break;
                if (command.equalsIgnoreCase("gui")) {
                    setRawMode(false);
                    launchGui();
                    return;
                }
                if (command.equalsIgnoreCase("help")) {
                    status = "+ - * / % ^ | sin cos tan | sqrt cbrt square reciprocal | log ln abs exp fact | pi e | gui exit";
                    continue;
                }
                if (command.isEmpty()) continue;
                try {
                    if (command.equalsIgnoreCase("pi") || command.equalsIgnoreCase("e")) {
                        double value = command.equalsIgnoreCase("pi") ? Math.PI : Math.E;
                        status = command + " = " + value;
                        recent.add(status);
                    } else if (isUnary(command)) {
                        String valueText = readEditableLine(command + " value", recent, inputHistory, status);
                        double value = Double.parseDouble(valueText);
                            double result = finite(unary(command, value));
                        status = command + "(" + value + ") = " + result;
                        recent.add(status);
                    } else if (command.matches("[+\\-*/%^]") || command.equalsIgnoreCase("mod")) {
                        String firstText = readEditableLine("first number", recent, inputHistory, status);
                        String secondText = readEditableLine("second number", recent, inputHistory, status);
                        double first = Double.parseDouble(firstText);
                        double second = Double.parseDouble(secondText);
                        String operator = command.equalsIgnoreCase("mod") ? "%" : command;
                            double result = finite(binary(operator, first, second));
                        status = first + " " + operator + " " + second + " = " + result;
                        recent.add(status);
                    } else {
                        status = "Unknown command. Type help for the operation list.";
                    }
                } catch (IllegalArgumentException exception) {
                    status = exception.getMessage() == null ? "Invalid value." : exception.getMessage();
                }
            }
        } catch (IOException exception) {
            System.err.println("Interactive terminal mode unavailable: " + exception.getMessage());
            runLineCli();
        } finally {
            try {
                setRawMode(false);
            } catch (IOException ignored) {
                // Terminal restoration is best effort during shutdown.
            }
            System.out.print("\033[0m\033[?25h\n");
        }
    }

    private static String readEditableLine(String label, List<String> recent,
            List<String> inputHistory, String status) throws IOException {
        StringBuilder value = new StringBuilder();
        InputStream input = System.in;
        int cursor = 0;
        int historyIndex = inputHistory.size();
        while (true) {
            drawTerminal(label, value, cursor, recent, status);
            int key = input.read();
            if (key < 0) return null;
            if (key == '\n' || key == '\r') {
                String completed = value.toString().trim();
                if (!completed.isEmpty()) inputHistory.add(completed);
                return completed;
            }
            if (key == 3) return "exit";
            if (key == 127 || key == 8) {
                if (cursor > 0) {
                    value.deleteCharAt(--cursor);
                }
                continue;
            }
            if (key == 27 && input.available() > 0 && input.read() == '[') {
                int arrow = input.read();
                if (arrow == 'D' && cursor > 0) cursor--;
                else if (arrow == 'C' && cursor < value.length()) cursor++;
                else if (arrow == 'A' && !inputHistory.isEmpty()) {
                    historyIndex = Math.max(0, historyIndex - 1);
                    value.setLength(0);
                    value.append(inputHistory.get(historyIndex));
                    cursor = value.length();
                } else if (arrow == 'B' && historyIndex < inputHistory.size() - 1) {
                    historyIndex++;
                    value.setLength(0);
                    value.append(inputHistory.get(historyIndex));
                    cursor = value.length();
                }
                continue;
            }
            if (key >= 32 && key < 127) {
                value.insert(cursor++, (char) key);
            }
        }
    }

    private static void drawTerminal(String label, StringBuilder value, int cursor,
            List<String> recent, String status) {
        StringBuilder output = new StringBuilder("\033[H\033[2J");
        output.append("\033[1;36m  calc\033[0m  ").append(status).append("\n\n");
        int start = Math.max(0, recent.size() - 4);
        for (int index = start; index < recent.size(); index++) {
            output.append("  \033[36m›\033[0m ").append(recent.get(index)).append("\n");
        }
        for (int index = recent.size() - start; index < 4; index++) output.append("\n");
        output.append("  \033[1;35m").append(label).append("\033[0m  ").append(value);
        output.append("\033[" ).append(Math.max(1, value.length() - cursor + 1)).append("D");
        System.out.print(output);
        System.out.flush();
    }

    private static void setRawMode(boolean enabled) throws IOException {
        String command = enabled ? "stty -icanon -echo" : "stty sane";
        Process process = new ProcessBuilder("sh", "-c", command + " < /dev/tty").inheritIO().start();
        try {
            if (process.waitFor() != 0) throw new IOException("stty failed");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("terminal mode interrupted", exception);
        }
    }

    private static void runLineCli() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("calc / cli");
        System.out.println("Type gui to open the desktop interface, or exit to quit.");
        while (true) {
            System.out.print("\ncalc> ");
            if (!scanner.hasNextLine()) break;
            String command = scanner.nextLine().trim();
            if (command.equalsIgnoreCase("gui")) {
                launchGui();
                break;
            }
            if (command.equalsIgnoreCase("exit")) break;
            if (command.equalsIgnoreCase("history")) {
                System.out.println("History is available in GUI mode.");
                continue;
            }
            try {
                if (command.equalsIgnoreCase("sin") || command.equalsIgnoreCase("cos")) {
                    System.out.print("degrees> ");
                    if (!scanner.hasNextLine()) break;
                    double value = Double.parseDouble(scanner.nextLine().trim());
                    double result = finite(unary(command, value));
                    System.out.println(command + "(" + value + ") = " + result);
                } else if (isUnary(command)) {
                    System.out.print("value> ");
                    if (!scanner.hasNextLine()) break;
                    double value = Double.parseDouble(scanner.nextLine().trim());
                    double result = finite(unary(command, value));
                    System.out.println(command + "(" + value + ") = " + result);
                } else if (command.equalsIgnoreCase("pi") || command.equalsIgnoreCase("e")) {
                    double result = command.equalsIgnoreCase("pi") ? Math.PI : Math.E;
                    System.out.println(command + " = " + result);
                } else if (command.matches("[+\\-*/%^]") || command.equalsIgnoreCase("mod")) {
                    System.out.print("first> ");
                    if (!scanner.hasNextLine()) break;
                    double first = Double.parseDouble(scanner.nextLine().trim());
                    System.out.print("second> ");
                    if (!scanner.hasNextLine()) break;
                    double second = Double.parseDouble(scanner.nextLine().trim());
                    double result;
                    String binaryOperator = command.equalsIgnoreCase("mod") ? "%" : command;
                    switch (binaryOperator) {
                        case "+": result = Addition.add(first, second); break;
                        case "-": result = Sub.subtract(first, second); break;
                        case "*": result = Multiplication.multiply(first, second); break;
                        case "%": result = first % second; break;
                        case "^": result = Math.pow(first, second); break;
                        default: result = Division.division(first, second); break;
                    }
                    System.out.println(first + " " + binaryOperator + " " + second + " = " + finite(result));
                } else {
                    System.out.println("Commands: + - * / % ^ sin cos tan sqrt log ln abs exp fact pi e gui exit");
                }
            } catch (IllegalArgumentException exception) {
                System.out.println(exception.getMessage() == null
                        ? "Please enter a valid number." : exception.getMessage());
            }
        }
        scanner.close();
    }

        private void showWindow() {
            JFrame frame = new JFrame("calc /");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(900, 590);
            frame.setMinimumSize(new Dimension(700, 500));
            frame.setLocationByPlatform(true);
            frame.setContentPane(createContent());
            frame.setVisible(true);
        }

        private JPanel createContent() {
            JPanel root = new JPanel(new BorderLayout(28, 0));
            root.setBackground(CANVAS);
            root.setBorder(BorderFactory.createEmptyBorder(30, 34, 30, 34));

            JPanel calculator = new JPanel(new BorderLayout(0, 22));
            calculator.setBackground(new Color(17, 29, 47));
            calculator.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(22, 22, 22, 22)));
            calculator.add(createHeader(), BorderLayout.NORTH);
            calculator.add(createKeypad(), BorderLayout.CENTER);
            root.add(calculator, BorderLayout.CENTER);
            root.add(createHistoryPanel(), BorderLayout.EAST);
            return root;
        }

        private JPanel createHeader() {
            JPanel header = new JPanel(new BorderLayout(0, 10));
            header.setOpaque(false);
            JLabel eyebrow = new JLabel("CALC  /  SCIENTIFIC");
            eyebrow.setFont(new Font("SansSerif", Font.BOLD, 11));
            eyebrow.setForeground(ACCENT);
            display.setHorizontalAlignment(SwingConstants.RIGHT);
            display.setEditable(true);
            display.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
            display.setBackground(new Color(13, 24, 41));
            display.setForeground(INK);
            display.setCaretColor(ACCENT);
            display.setFont(new Font("Monospaced", Font.PLAIN, 38));
            display.setPreferredSize(new Dimension(0, 86));
            display.addActionListener(event -> commitTypedValue());
            display.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent event) {
                    display.selectAll();
                }
            });
            header.add(eyebrow, BorderLayout.NORTH);
            header.add(display, BorderLayout.CENTER);
            return header;
        }

        private JPanel createKeypad() {
            JPanel keypad = new JPanel(new GridLayout(7, 5, 10, 10));
            keypad.setOpaque(false);
            addButton(keypad, "C", event -> clear(), true);
            addButton(keypad, "DEL", event -> deleteLast(), false);
            addButton(keypad, "sin", event -> calculateUnary("sin"), false);
            addButton(keypad, "cos", event -> calculateUnary("cos"), false);
            addButton(keypad, "tan", event -> calculateUnary("tan"), false);
            addButton(keypad, "sqrt", event -> calculateUnary("sqrt"), false);
            addButton(keypad, "cbrt", event -> calculateUnary("cbrt"), false);
            addButton(keypad, "x²", event -> calculateUnary("square"), false);
            addButton(keypad, "1/x", event -> calculateUnary("reciprocal"), false);
            addButton(keypad, "%", event -> chooseOperator("%"), false);
            addButton(keypad, "7", event -> append("7"), false);
            addButton(keypad, "8", event -> append("8"), false);
            addButton(keypad, "9", event -> append("9"), false);
            addButton(keypad, "/", event -> chooseOperator("/"), false);
            addButton(keypad, "4", event -> append("4"), false);
            addButton(keypad, "5", event -> append("5"), false);
            addButton(keypad, "6", event -> append("6"), false);
            addButton(keypad, "*", event -> chooseOperator("*"), false);
            addButton(keypad, "^", event -> chooseOperator("^"), false);
            addButton(keypad, "log", event -> calculateUnary("log"), false);
            addButton(keypad, "1", event -> append("1"), false);
            addButton(keypad, "2", event -> append("2"), false);
            addButton(keypad, "3", event -> append("3"), false);
            addButton(keypad, "-", event -> chooseOperator("-"), false);
            addButton(keypad, "ln", event -> calculateUnary("ln"), false);
            addButton(keypad, "abs", event -> calculateUnary("abs"), false);
            addButton(keypad, "exp", event -> calculateUnary("exp"), false);
            addButton(keypad, "0", event -> append("0"), false);
            addButton(keypad, ".", event -> append("."), false);
            addButton(keypad, "=", event -> calculateResult(), true);
            addButton(keypad, "+", event -> chooseOperator("+"), false);
            addButton(keypad, "±", event -> calculateUnary("negate"), false);
            addButton(keypad, "fact", event -> calculateUnary("fact"), false);
            addButton(keypad, "π", event -> setConstant(Math.PI), false);
            addButton(keypad, "e", event -> setConstant(Math.E), false);
            return keypad;
        }

        private JPanel createHistoryPanel() {
            JPanel panel = new JPanel(new BorderLayout(0, 12));
            panel.setOpaque(false);
            panel.setPreferredSize(new Dimension(255, 0));
            JLabel title = new JLabel("HISTORY");
            title.setFont(new Font("SansSerif", Font.BOLD, 11));
            title.setForeground(MUTED);
            JList<String> list = new JList<>(history);
            list.setFont(new Font("SansSerif", Font.PLAIN, 14));
            list.setForeground(INK);
            list.setBackground(new Color(13, 24, 41));
            list.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            list.setFixedCellHeight(30);
            JScrollPane scroll = new JScrollPane(list);
            scroll.setBorder(BorderFactory.createLineBorder(LINE));
            scroll.getViewport().setBackground(new Color(13, 24, 41));
            panel.add(title, BorderLayout.NORTH);
            panel.add(scroll, BorderLayout.CENTER);
            return panel;
        }

        private void addButton(JPanel keypad, String label, ActionListener action, boolean accented) {
            JButton button = new JButton(label);
            button.setFont(new Font("SansSerif", Font.BOLD, 16));
            button.setForeground(accented ? new Color(7, 35, 34) : INK);
            button.setBackground(accented ? ACCENT : new Color(24, 40, 63));
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createLineBorder(accented ? ACCENT : LINE));
            button.setMargin(new Insets(0, 0, 0, 0));
            button.addActionListener(action);
            keypad.add(button);
        }

        private void append(String value) {
            if (waitingForOperand) {
                display.setText(value.equals(".") ? "0." : value);
                waitingForOperand = false;
                return;
            }
            if (value.equals(".") && display.getText().contains(".")) return;
            display.setText(display.getText().equals("0") && !value.equals(".")
                    ? value : display.getText() + value);
        }

        private void chooseOperator(String operator) {
            try {
                if (firstOperand != null && !waitingForOperand) calculateResult();
                firstOperand = Double.parseDouble(display.getText());
                pendingOperator = operator;
                waitingForOperand = true;
            } catch (NumberFormatException exception) {
                display.setText("Error");
            }
        }

        private void calculateResult() {
            if (firstOperand == null || pendingOperator == null) return;
            try {
                double secondOperand = Double.parseDouble(display.getText());
                double result;
                switch (pendingOperator) {
                    case "+": result = Addition.add(firstOperand, secondOperand); break;
                    case "-": result = Sub.subtract(firstOperand, secondOperand); break;
                    case "*": result = Multiplication.multiply(firstOperand, secondOperand); break;
                    case "%": result = firstOperand % secondOperand; break;
                    case "^": result = Math.pow(firstOperand, secondOperand); break;
                    default: result = Division.division(firstOperand, secondOperand); break;
                }
                record(firstOperand + " " + pendingOperator + " " + secondOperand + " = " + result);
                display.setText(format(result));
                firstOperand = null;
                pendingOperator = null;
                waitingForOperand = true;
            } catch (NumberFormatException exception) {
                display.setText("Error");
            }
        }

        private void calculateUnary(String operator) {
            try {
                double input = Double.parseDouble(display.getText());
                    double result = finite(unary(operator, input));
                record(operator + "(" + input + ") = " + result);
                display.setText(format(result));
                waitingForOperand = true;
            } catch (IllegalArgumentException exception) {
                display.setText("Error");
            }
        }

        private static boolean isUnary(String operator) {
            return operator.equalsIgnoreCase("tan") || operator.equalsIgnoreCase("sqrt")
                    || operator.equalsIgnoreCase("cbrt") || operator.equalsIgnoreCase("square")
                    || operator.equalsIgnoreCase("reciprocal") || operator.equalsIgnoreCase("log")
                    || operator.equalsIgnoreCase("ln") || operator.equalsIgnoreCase("abs")
                    || operator.equalsIgnoreCase("exp") || operator.equalsIgnoreCase("negate")
                    || operator.equalsIgnoreCase("fact") || operator.equalsIgnoreCase("factorial")
                    || operator.equalsIgnoreCase("asin") || operator.equalsIgnoreCase("acos")
                    || operator.equalsIgnoreCase("atan") || operator.equalsIgnoreCase("sin")
                    || operator.equalsIgnoreCase("cos");
        }

        private static double unary(String operator, double value) {
            switch (operator.toLowerCase()) {
                case "sin": return Sin.calculate(value);
                case "cos": return Cos.calculate(value);
                case "tan": return Math.tan(Math.toRadians(value));
                case "asin": return Math.toDegrees(Math.asin(value));
                case "acos": return Math.toDegrees(Math.acos(value));
                case "atan": return Math.toDegrees(Math.atan(value));
                case "sqrt": return Math.sqrt(value);
                case "cbrt": return Math.cbrt(value);
                case "square": return value * value;
                case "reciprocal": return 1 / value;
                case "log": return Math.log10(value);
                case "ln": return Math.log(value);
                case "abs": return Math.abs(value);
                case "exp": return Math.exp(value);
                case "negate": return -value;
                case "fact":
                case "factorial": return factorial(value);
                default: throw new IllegalArgumentException("Unknown operation");
            }
        }

        private static double factorial(double value) {
            if (value < 0 || value != Math.floor(value)) {
                throw new IllegalArgumentException("Factorial needs a non-negative integer");
            }
            double result = 1;
            for (int number = 2; number <= value; number++) result *= number;
            return result;
        }

        private void setConstant(double value) {
            display.setText(format(value));
            waitingForOperand = true;
        }

        private void commitTypedValue() {
            try {
                if (firstOperand != null && pendingOperator != null) {
                    calculateResult();
                } else {
                    display.setText(format(Double.parseDouble(display.getText())));
                    waitingForOperand = true;
                }
            } catch (NumberFormatException exception) {
                display.setText("Error");
            }
        }

        private void record(String calculation) {
            history.addElement(calculation);
        }

        private void deleteLast() {
            String value = display.getText();
            if (value.length() <= 1 || value.equals("Error")) display.setText("0");
            else display.setText(value.substring(0, value.length() - 1));
        }

        private void clear() {
            display.setText("0");
            firstOperand = null;
            pendingOperator = null;
            waitingForOperand = false;
        }

        private String format(double value) {
            return value == (long) value ? Long.toString((long) value) : Double.toString(value);
        }

    private static double binary(String operator, double first, double second) {
        switch (operator) {
            case "+": return Addition.add(first, second);
            case "-": return Sub.subtract(first, second);
            case "*": return Multiplication.multiply(first, second);
            case "%": return first % second;
            case "^": return Math.pow(first, second);
            case "/": return Division.division(first, second);
            default: throw new IllegalArgumentException("Unknown binary operator");
        }
    }

    private static double finite(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Result is outside the real number range");
        }
        return value;
    }

}
