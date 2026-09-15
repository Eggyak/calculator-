import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        List<String> history = new ArrayList<>();

        System.out.println("Calculator (angles for sin/cos are in degrees)");
        System.out.println("Operators: +, -, *, /, sin, cos, history, exit");

        while (true) {
            System.out.print("Enter an operator: ");
            String operator = scanner.nextLine().trim().toLowerCase();

            if (operator.equals("exit")) {
                break;
            }

            if (operator.equals("history")) {
                if (history.isEmpty()) {
                    System.out.println("No calculations yet.");
                } else {
                    history.forEach(System.out::println);
                }
                continue;
            }

            try {
                double firstNumber;
                double result;
                String calculation;

                System.out.print("Enter the first number: ");
                firstNumber = Double.parseDouble(scanner.nextLine().trim());

                if (operator.equals("sin") || operator.equals("cos")) {
                    result = operator.equals("sin")
                            ? Sin.calculate(firstNumber)
                            : Cos.calculate(firstNumber);
                    calculation = operator + "(" + firstNumber + ") = " + result;
                } else {
                    System.out.print("Enter the second number: ");
                    double secondNumber = Double.parseDouble(scanner.nextLine().trim());

                    switch (operator) {
                        case "+":
                            result = Addition.add(firstNumber, secondNumber);
                            break;
                        case "-":
                            result = Sub.subtract(firstNumber, secondNumber);
                            break;
                        case "*":
                            result = Multiplication.multiply(firstNumber, secondNumber);
                            break;
                        case "/":
                            result = Division.division(firstNumber, secondNumber);
                            break;
                        default:
                            System.out.println("Unknown operator.");
                            continue;
                    }
                    calculation = firstNumber + " " + operator + " " + secondNumber + " = " + result;
                }

                System.out.println(calculation);
                history.add(calculation);
            } catch (NumberFormatException exception) {
                System.out.println("Please enter valid numbers.");
            }
        }

        scanner.close();
    }
}
