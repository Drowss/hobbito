package adapter.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private String username;
    private String look;
    private String motto;
    private Long diamonds;
    private Long pixels;
    private Long respects;
    private Long battlepass;
}
