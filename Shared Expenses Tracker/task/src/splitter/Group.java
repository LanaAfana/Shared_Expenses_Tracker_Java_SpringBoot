package splitter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Group {
    private String name;
    private List<String> participants;

    public static boolean isAGroup(String name) {
        return name.matches("^[A-Z]+$");
    }

    public static boolean isValidOperation(String cmdStr, String cmd) {
        String[] cmdList = cmdStr.split("\\s+");
        if (cmdList[1].equals(cmd)
            && Group.isAGroup(cmdList[2])) {
            return true;
        }
        return false;
    }
}