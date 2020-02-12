import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class Algorithm {

    private Algorithm(){}

    /**
     * Count how many adjacent elements fulfills pred
     * @param col Collection in which to search
     * @param pred BiPredicate
     * @param <T> Element type
     * @return Number of pairs fulfilling predicate
     */
    public static <T> int count_comp(Iterable<T> col, BiPredicate<T, T> pred){

        int res = 0;
        Iterator<T> it = col.iterator();
        if(!it.hasNext()) return 0;
        T elem = it.next();
        while(it.hasNext()){

            T nxt;
            if(pred.test(nxt = it.next(), elem)) ++res;
            elem = nxt;

        }

        return res;

    }

    /**
     * Count number of elements fulfilling predicate
     * @param col Collection in which to search
     * @param pred Predicate
     * @param <T> Element type
     * @return Number of elements fulfilling predicate
     */
    public static <T> int count_if(Iterable<T> col, Predicate<T> pred){

        int res = 0;
        Iterator<T> it = col.iterator();
        if(!it.hasNext()) return 0;
        while(it.hasNext()){

            T elem = it.next();
            if(pred.test(elem)) ++res;

        }

        return res;

    }

    //Turns out String is not Iterable
    public static <T> int count_if(String col, Predicate<Character> pred){

        int res = 0;
        if(col.length() == 0) return 0;
        for(char c:col.toCharArray()){

            if(pred.test(c)) ++res;

        }

        return res;

    }

    /**
     * Convert byte[4] to int, byte[0] is highest int byte
     * @param bytes
     * @return int
     */
    public static int btoi(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) |
                ((bytes[1] & 0xFF) << 16) |
                ((bytes[2] & 0xFF) << 8 ) |
                ((bytes[3] & 0xFF) << 0 );
    }

    public static byte[] itob(int i){

        byte b[] = new byte[4];
        b[0] = (byte)((i >> 24) & 0xFF);
        b[1] = (byte)((i >> 16) & 0xFF);
        b[2] = (byte)((i >> 8) & 0xFF);
        b[3] = (byte)((i >> 0) & 0xFF);
        return b;

    }


    @Test
    void testCount_if(){

        String str = "ahgfawhhajdskhhhlfa";
        assertTrue(Algorithm.count_if(str, (Character c) -> { return c.equals('a'); }) == 4);
        assertTrue(Algorithm.count_if(str, (Character c) -> { return c.equals('h'); }) == 6);

    }

}
