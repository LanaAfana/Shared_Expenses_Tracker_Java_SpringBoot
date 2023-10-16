package splitter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "GROUPS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Group {

    @Id
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> participants;

    public static boolean isAGroup(String name) {
        return name.matches("^[A-Z]+$");
    }

    public static boolean isValidOperation(String cmdStr, String cmd) {
        String[] cmdList = cmdStr.split("\\s+");
        return cmdList[1].equals(cmd)
                && Group.isAGroup(cmdList[2]);
    }
}