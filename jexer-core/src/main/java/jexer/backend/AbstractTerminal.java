package jexer.backend;

public abstract class AbstractTerminal {
	
	protected Object listener;

    public void setListener(Object listener) {
		this.listener = listener;
	}
    
}
