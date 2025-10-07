import javacore.CustomLinkedList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomLinkedListTest {

    private CustomLinkedList<String> customLinkedList;

    @BeforeEach
    void setUp() {
        customLinkedList = new CustomLinkedList<>();
        customLinkedList.addFirst("1");
        customLinkedList.addLast("2");
        customLinkedList.addLast("3");
    }

    @Test
    void testAddFirst() {
        customLinkedList.addFirst("0");
        assertEquals("0", customLinkedList.get(0));
        assertEquals(4, customLinkedList.size());
    }

    @Test
    void testAddLast() {
        customLinkedList.addLast("4");
        assertEquals("4", customLinkedList.get(customLinkedList.size() - 1));
        assertEquals(4, customLinkedList.size());
    }

    @Test
    void testAddByIndex() {
        customLinkedList.add(1, "1.5");
        assertEquals("1.5", customLinkedList.get(1));
        assertEquals("2", customLinkedList.get(2));
        assertEquals(4, customLinkedList.size());
    }

    @Test
    void testAddByIndexEqualsZero() {
        customLinkedList.add(0, "0.5");
        assertEquals("0.5", customLinkedList.getFirst());
        assertEquals("1", customLinkedList.get(1));
        assertEquals(4, customLinkedList.size());
    }

    @Test
    void testAddAtEndByIndex() {
        customLinkedList.add(3, "4");
        assertEquals("4", customLinkedList.getLast());
        assertEquals(4, customLinkedList.size());
    }

    @Test
    void testAddThrowsIndexOutOfBoundsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> customLinkedList.add(-1, "1"));
        assertThrows(IndexOutOfBoundsException.class, () -> customLinkedList.add(10, "11"));
    }

    @Test
    void testGetFirst() {
        assertEquals("1", customLinkedList.getFirst());
    }

    @Test
    void testGetLast() {
        assertEquals("3", customLinkedList.getLast());
    }

    @Test
    void testGetByIndex() {
        assertEquals("2", customLinkedList.get(1));
    }

    @Test
    void testGetThrowsIndexOutOfBoundsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> customLinkedList.get(5));
    }

    @Test
    void testRemoveFirst() {
        customLinkedList.removeFirst();
        assertEquals("2", customLinkedList.getFirst());
        assertEquals(2, customLinkedList.size());
    }

    @Test
    void testRemoveLast() {
        customLinkedList.removeLast();
        assertEquals("2", customLinkedList.getLast());
        assertEquals(2, customLinkedList.size());
    }

    @Test
    void testRemoveByIndex() {
        customLinkedList.remove(1);
        assertEquals("3", customLinkedList.get(1));
        assertEquals(2, customLinkedList.size());
    }

    @Test
    void TestRemoveOnLastElement() {
        customLinkedList.removeFirst();
        customLinkedList.removeFirst();
        customLinkedList.removeFirst();

        assertEquals(0, customLinkedList.size());
    }

    @Test
    void testRemoveFirstThrowsNoSuchElementException() {
        customLinkedList.removeFirst();
        customLinkedList.removeFirst();
        customLinkedList.removeFirst();

        assertThrows(NoSuchElementException.class, () -> customLinkedList.removeFirst());
    }
}
