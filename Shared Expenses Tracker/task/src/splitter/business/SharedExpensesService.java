package splitter.business;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import splitter.Group;
import splitter.Transaction;
import splitter.persistence.GroupRepository;
import splitter.persistence.TransactionRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class SharedExpensesService {

    private final GroupRepository groupRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    public SharedExpensesService(GroupRepository groupRepository, TransactionRepository transactionRepository) {
        this.groupRepository = groupRepository;
        this.transactionRepository = transactionRepository;
    }

    public Optional<Group> findGroupByName(String name) {
        return groupRepository.findByName(name)
                .stream()
                .filter(x -> x.getName().equals(name))
                .findFirst();
    }

    public void saveGroup(Group group) {
        groupRepository.save(group);
    }
    public void deleteGroupByName(String name) {
        groupRepository.deleteByName(name);
    }

    public Transaction saveTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public void deleteTransactionAfterDate(LocalDate date) {
        transactionRepository.deleteByDateLessThanEqual(date);
    }
}
