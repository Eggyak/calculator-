import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Take inputs from the user
        System.out.print("Enter the first number: ");
        double num1 = scanner.nextDouble();

        System.out.print("Enter the second number: ");
        double num2 = scanner.nextDouble();

        // Call functions from all 4 separate files
        double sum = Addition.add(num1, num2);
        double difference = Sub.subtract(num1, num2);
        double product = Multiplication.multiply(num1, num2);
        double quotient = Division.division(num1, num2);

        // Display all results
        System.out.println("\n--- Results ---");
        System.out.println("Addition (Sum): " + sum);
        System.out.println("Subtraction (Difference): " + difference);
        System.out.println("Multiplication (Product): " + product);
        System.out.println("Division (Quotient): " + quotient);

        scanner.close();
    }
}
