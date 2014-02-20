package src.main;

import java.util.ArrayList;

public class MyNode {
	
	String text = "";
	ArrayList<MyNode> subList = new ArrayList<MyNode>();
	
	public MyNode (String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	
	public ArrayList<MyNode> getSubList() {
		return subList;
	}
	
	public void addToSubList (MyNode n) {
		subList.add(n);
	}
}
