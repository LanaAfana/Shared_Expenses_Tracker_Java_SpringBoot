package splitter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "TRANSACTIONS")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Transaction implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    private LocalDate date;

    private String acc_debit;

    private String acc_credit;

    private Double sum;

    public Transaction(LocalDate date,
                       String acc_debit,
                       String acc_credit,
                       Double sum) {
        this.date = date;
        this.acc_debit = acc_debit;
        this.acc_credit = acc_credit;
        this.sum = sum;
    }

    static public boolean isValidP2P(String[] cmdList, String command) {
        return cmdList.length >= 4
                && (cmdList.length != 4 || cmdList[0].equals(command))
                && (cmdList.length != 5 || (cmdList[1].equals(command) || cmdList[0].equals(command)))
                && (cmdList.length != 6 || cmdList[1].equals(command));
    }

    static public boolean isValidP2G(String cmd, String command) {
//        String[] cmdParts = cmd.split("\\(");
//        String[] cmdList = cmdParts[0].trim().split("\\s+");
        return true;
    }



    static public boolean isValidBalance(String[] cmdList) {
        return (cmdList.length != 1 || cmdList[0].equals("balance"))
                && (cmdList.length != 2 || (cmdList[0].equals("balance") || cmdList[1].equals("balance")))
                && (cmdList.length != 3 || (cmdList[1].equals("balance") && cmdList[2].matches("open|close")));
    }
}
