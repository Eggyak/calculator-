import java.util.*;

class Calculator{
    int a;
    int b;
    String op;
    double ans;

    public void parse(int a, String op, int b){
        this.a = a;
        this.b = b;
        this.op = op;
        switch (op){
            case "+":
                this.ans = add(this.a, this.b);
                display(a,op,b,ans);
                break;
        }
    }

    private double add(int a, int b){
        return (double)(a+b);
    }

    public void display(int a, String op, int b, double ans){
        System.out.println(a + " " + op + " " + b + " = " + ans);
    }

}

public class Main{
    public static void main(String[] args){
        System.out.println("This is a calculator Application.");
        Calculator calc = new Calculator();
        calc.parse(5, "+", 6);
    }
}