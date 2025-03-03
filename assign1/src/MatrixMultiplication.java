import java.util.Scanner;

public class MatrixMultiplication {

    public static void onMultLine(int mAr, int mBr) {
        double[][] pha = new double[mAr][mAr];
        double[][] phb = new double[mAr][mAr];
        double[][] phc = new double[mAr][mAr];
        
        for (int i=0; i<mAr; i++)
            for (int j=0; j<mAr; j++)
                pha[i][j] = 1.0;

        for (int i=0; i<mBr; i++)
            for (int j=0; j<mBr; j++)
                phb[i][j]=i + 1;

        long start=System.nanoTime();
        for (int i=0; i<mAr; i++) {
            for (int k=0; k<mAr; k++) {
                double temp = pha[i][k];
                for (int j=0; j<mBr; j++) {
                    phc[i][j]+=temp*phb[k][j];
                }
            }
        }

        long end=System.nanoTime();
        System.out.printf("Time: %.3f seconds\n", (end-start)/1e9);
        System.out.println("Result matrix:");
        for (int j = 0; j < Math.min(10, mBr); j++) {
            System.out.print(phc[0][j] + " ");
        }
        System.out.println();
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int option;

        do {
            System.out.println("\n1. Multiplication");
            System.out.println("2. Line Multiplication");
            System.out.print("Selection?: ");
            option=scanner.nextInt();
            if(option==0){break;}
            System.out.print("Dimensions (rows=cols)?: ");
            int size=scanner.nextInt();
            switch(option){
                case 1:
                    //onMult(size, size);
                    break;
                case 2:
                    onMultLine(size, size);
                    break;
            }
        } while(option!=0);     
        scanner.close();
    }

}