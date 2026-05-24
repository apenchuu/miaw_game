package com.steven.frontend.patterns.command;

// Menjalankan command yang diterima.
public class Invoker {
    public void invoke(ICommand command) {
        if (command != null) {
            command.execute();
        }
    }
}
