package common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HobbaUserCode {
    private String hobbaUsername;
    private String hobbaVerificationCode;
}
