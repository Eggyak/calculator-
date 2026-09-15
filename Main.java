import java.util.*;
import 

public class Main{
    public static void main(String[] args){
        System.out.println("This is a calculator Application.");
        Calculator calc = new Calculator();
        calc.parse(5, "+", 6);
    }
}