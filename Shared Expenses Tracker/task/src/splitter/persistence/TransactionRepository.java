package splitter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import splitter.Transaction;

import java.time.LocalDate;
import java.util.List;

@Transactional
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    public List<Transaction> deleteByDateLessThanEqual(LocalDate date);

}
