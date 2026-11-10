public class LinkedQueue<T> {

private Node<T> firstNode;
private Node<T> backNode;
private int numberOfEntries;

private static class Node<T>{

    private T data;
    private Node <T> next;

    public Node(T data){
        this.data = data;
        this.next = null;
    }
}

public LinkedQueue(){

    firstNode = null;
    backNode = null;
    numberOfEntries = 0;
}
public void enqueue(T newEntry){
    Node<T> newNode = new Node<T>(newEntry);
    if(this.isEmpty()){
        firstNode = newNode;
        
    }
    else{
        backNode.next = newNode;
        }
    backNode = newNode;
    numberOfEntries ++;

}
public T dequeue(){
    if(this.isEmpty()){
        throw new java.util.NoSuchElementException();
    }
    T removed = firstNode.data;
    firstNode = firstNode.next;
    numberOfEntries --;
    if (numberOfEntries == 0){
        backNode = null;
    }
    return removed;
    
}

public boolean isEmpty(){
    return numberOfEntries == 0;
}

public int size(){
    return numberOfEntries;
}

}
    

