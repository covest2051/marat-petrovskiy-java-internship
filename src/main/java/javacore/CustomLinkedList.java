package javacore;

import java.util.NoSuchElementException;

public class CustomLinkedList<E> {
    private int size = 0;

    private Node<E> first;

    private Node<E> last;

    public CustomLinkedList() {
    }

    public int size() {
        return size;
    }

    public void addFirst(E element) {
        final CustomLinkedList.Node<E> currentFirstNode = first;
        final CustomLinkedList.Node<E> newFirstNode = new CustomLinkedList.Node<>(null, element, currentFirstNode);

        first = newFirstNode;

        if (currentFirstNode == null) {
            last = newFirstNode;
        } else {
            currentFirstNode.prev = newFirstNode;
        }
        size++;
    }

    public void addLast(E element) {
        final CustomLinkedList.Node<E> currentLastNode = last;
        final CustomLinkedList.Node<E> newLastNode = new CustomLinkedList.Node<>(currentLastNode, element, null);

        last = newLastNode;

        if (currentLastNode == null) {
            first = newLastNode;
        } else {
            currentLastNode.next = newLastNode;
        }
        size++;
    }

    public void add(int index, E element) {
        if (isAcceptableIndex(index) || index == size) {
            if (index == 0) {
                addFirst(element);
                return;
            }

            if (index == size) {
                addLast(element);
                return;
            }

            final Node<E> node = findNodeByIndex(index);
            final CustomLinkedList.Node<E> prevNode = node.prev;

            final CustomLinkedList.Node<E> newNode = new Node<>(prevNode, element, node);

            prevNode.next = newNode;
            node.prev = newNode;

            size++;
        } else {
            throw new IndexOutOfBoundsException("Index: [" + index + "] out of list bounds");
        }
    }

    public E getFirst() {
        final CustomLinkedList.Node<E> firstElement = first;
        if (firstElement == null)
            throw new NoSuchElementException();
        return firstElement.item;
    }

    public E getLast() {
        final CustomLinkedList.Node<E> lastElement = last;
        if (lastElement == null)
            throw new NoSuchElementException();
        return lastElement.item;
    }

    public E get(int index) {
        if (isAcceptableIndex(index)) {
            return findNodeByIndex(index).item;
        } else {
            throw new IndexOutOfBoundsException("Index: [" + index + "] out of list bounds");
        }
    }

    public void removeFirst() {
        final CustomLinkedList.Node<E> currentFirstElement = first;
        if (currentFirstElement == null)
            throw new NoSuchElementException();

        final CustomLinkedList.Node<E> newFirstElement = currentFirstElement.next;

        currentFirstElement.item = null;
        currentFirstElement.next = null;
        currentFirstElement.prev = null;

        first = newFirstElement;

        if (newFirstElement == null) {
            last = null;
        } else {
            newFirstElement.prev = null;
        }

        size--;
    }

    public void removeLast() {
        final CustomLinkedList.Node<E> currentLastElement = last;
        if (currentLastElement == null)
            throw new NoSuchElementException();

        final CustomLinkedList.Node<E> newLastElement = currentLastElement.prev;

        currentLastElement.item = null;
        currentLastElement.next = null;
        currentLastElement.prev = null;

        last = newLastElement;

        if (newLastElement == null) {
            first = null;
        } else {
            newLastElement.next = null;
        }

        size--;
    }

    public void remove(int index) {
        if (isAcceptableIndex(index)) {
            final Node<E> node = findNodeByIndex(index);

            final CustomLinkedList.Node<E> prevNode = node.prev;
            final CustomLinkedList.Node<E> nextNode = node.next;

            if (prevNode == null) {
                first = nextNode;
            } else {
                prevNode.next = nextNode;
                node.prev = null;
            }

            if (nextNode == null) {
                last = prevNode;
            } else {
                nextNode.prev = prevNode;
                node.next = null;
            }

            node.item = null;

            size--;
        } else {
            throw new IndexOutOfBoundsException("Index: [" + index + "] out of list bounds");
        }
    }

    private static class Node<E> {
        E item;
        CustomLinkedList.Node<E> next;
        CustomLinkedList.Node<E> prev;

        Node(CustomLinkedList.Node<E> prev, E element, CustomLinkedList.Node<E> next) {
            this.item = element;
            this.next = next;
            this.prev = prev;
        }
    }

    private boolean isAcceptableIndex(int index) {
        return index >= 0 && index < size;
    }

    private Node<E> findNodeByIndex(int index) {
        Node<E> node;
        if (index < (size / 2)) {
            node = first;
            for (int i = 0; i < index; i++) {
                node = node.next;
            }
        } else {
            node = last;
            for (int i = size - 1; i > index; i--) {
                node = node.prev;
            }
        }
        return node;
    }
}
