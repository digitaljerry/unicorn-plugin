/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package si.nej.hudson.plugins;

/**
 *
 * @author jernejz
 */
public class Observer {

    private String id;
    private String name;
    private int errors;
    private int warnings;

    Observer() {}
    
    public Observer(String name) {
        this.name = name;
    }

    public Observer(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Observer(String id, String name, int errors, int warnings) {
        this.id = id;
        this.name = name;
        this.errors = errors;
        this.warnings = warnings;
    }

    Observer(String id, String name, String errors, String warnings) {
        this.id = id;
        this.name = name;

        try {
            this.errors = Integer.parseInt(errors);
        } catch(NumberFormatException e) {
            this.errors = 0;
        }
        
        try {
            this.warnings = Integer.parseInt(warnings);
        } catch(NumberFormatException e) {
            this.warnings = 0;
        }
    }
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getErrors() {
        return errors;
    }

    public void setErrors(int errors) {
        this.errors = errors;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWarnings() {
        return warnings;
    }

    public void setWarnings(int warnings) {
        this.warnings = warnings;
    }
    
    @Override
    public String toString() {
        String output = "";

        output += name + " (" + id + ")\n";
        output += "Number of errors   : " + errors + "\n";
        output += "Number of warnings : " + warnings + "\n";
        output += "\n";

        return output;
    }

}
