package spring.board.dto.board;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
public class BoardDto {

    @NotEmpty(message = "제목을 입력해주세요.")
    private String title;
    @NotEmpty(message = "내용을 입력해주세요.")
    private String content;

    private List<MultipartFile> imageFiles;

    public BoardDto() {}

    public BoardDto(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public BoardDto(String title, String content, List<MultipartFile> imageFiles) {
        this.title = title;
        this.content = content;
        this.imageFiles = imageFiles;
    }
}

