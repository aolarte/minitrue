package com.andresolarte.proles;

public class Test {

    public static void main(String[] args) {
        int x=2;
        int y=2;

        System.out.println("Example #1=>  2 + 2: " + (x+y) + " (Using variables)");

        Integer bigX=2;
        Integer bigY=2;
        System.out.println("Example #2=>  2 + 2: " + (bigX+bigY) + " (Using boxed variables)");

        int z=2+2;
        System.out.println("Example #3=>  2 + 2: " + z + " (Using constants)");

        System.out.println("Example #4=>  2 + 2: " + (2+2) + " (Using constants and string concatenation)");
    }

}
