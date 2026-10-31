import com.anas.strata.MemTable;
import com.anas.strata.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MemTableTest {
    @Test
    void getReturnsValueAfterPut() {
        MemTable table = new MemTable();
        table.put("Raj", "97xxx");

        assertEquals("97xxx", table.get("Raj"));
    }

    @Test
    void getThrowsAfterDelete() {
        MemTable table = new MemTable();
        table.put("Raj", "97xxx");
        table.delete("Raj");

        assertThrows(ResourceNotFoundException.class, () -> table.get("Raj"));
    }

    @Test
    void getThrowsForMissingKey() {
        MemTable table = new MemTable();

        assertThrows(ResourceNotFoundException.class, () -> table.get("Nobody"));
    }
}
