package ports;

import org.dimitri.user.domain.User;
import org.springframework.boot.autoconfigure.security.SecurityProperties;

public interface UserWritePort {
    void save(User user);
}
