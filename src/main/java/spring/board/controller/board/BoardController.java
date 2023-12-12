package spring.board.controller.board;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import spring.board.domain.Board;
import spring.board.domain.Member;
import spring.board.dto.board.BoardDto;
import spring.board.dto.board.ContentDetailDto;
import spring.board.dto.board.SearchCondition;
import spring.board.dto.board.SearchContentDto;
import spring.board.dto.boardcomment.FindCommentDto;

import spring.board.dto.files.FilesResponseDto;
import spring.board.service.BoardCommentService;
import spring.board.service.BoardService;
import spring.board.service.FilesService;


import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class BoardController {

    @Value("${file.dir}")
    private String fileDir;

    private final BoardService boardService;
    private final BoardCommentService boardCommentService;
    private final FilesService filesService;

    //글작성 페이지
    @GetMapping("/board")
    public String board(Model model){
        model.addAttribute("form", new BoardDto());
        return "board/board";
    }

    @Transactional
    @PostMapping("/createContent")
    public String createContent(@Validated @ModelAttribute("form") BoardDto boardDto,
                                BindingResult bindingResult) throws IOException {

        if(bindingResult.hasErrors()){
            return "board/board";
        }

        Board savedBoard = boardService.save(boardDto, getAuthMember().getUsername());
        log.info("savedBoard.getId() = {}",savedBoard.getId());

        if(boardDto.getImageFiles().stream().anyMatch(file -> !file.isEmpty())){
            filesService.save(boardDto, savedBoard);
        }

        return "redirect:/contents";
    }

    //@GetMapping("/contents")
    public String contents(Model model){
        List<SearchContentDto> contents = boardService.findContent();
        model.addAttribute("contents",contents);
        return "board/contents";
    }

    // 폼으로 댓글 정보 얻어옴 SaveCommentDto
    // contentId를 이용해 Board와 member객체를 찾음
    @GetMapping("/contents/{contentId}")
    public String contentDetail(@PathVariable("contentId") Long contentId, Model model){
        ContentDetailDto findContentDetail = boardService.findContentDetail(contentId);
        model.addAttribute("form",findContentDetail);

        model.addAttribute("contentId",contentId);

        List<FindCommentDto> comments = boardCommentService.findComments(contentId);
        log.info("comments.size()={}",comments.size());

        /*if(!comments.isEmpty()){
            model.addAttribute("comments",comments); // 여기에 댓글들을 담아서 보냄
            // commentId, boardId, nickname, comment, lastUpdateTime
        }*/

        model.addAttribute("comments",comments); // 여기에 댓글들을 담아서 보냄
        // commentId, boardId, nickname, comment, lastUpdateTime


        List<FilesResponseDto> files = filesService.findFiles(contentId);
        if(!files.isEmpty()){
            for (FilesResponseDto file : files) {
                log.info("file={}",file.getStoreFileName());
            }
            model.addAttribute("imageFiles",files);
        }

        String boardUser = boardService.findById(contentId).getMember().getUsername();
        model.addAttribute("userId",namesEq(boardUser));

        return "board/content";
    }

    @ResponseBody
    @GetMapping("/images/{filename}")
    public Resource downloadImage(@PathVariable String filename) throws MalformedURLException {
        return new UrlResource("file:" + fileDir + filename);
    }

    private static Member getAuthMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Member member = (Member) authentication.getPrincipal();
        return member;
    }

    private static boolean namesEq (String boardUser){
        String username = getAuthMember().getUsername();
        if(username.equals(boardUser)){
            return true;
        }
        return false;
    }


    @GetMapping("/content/{contentId}/edit")
    public String editContent(@PathVariable Long contentId, Model model){
        Board findBoard = boardService.findById(contentId);

        if(!namesEq(findBoard.getMember().getUsername())){
            model.addAttribute("message","다른 사용자의 글입니다.");
            model.addAttribute("searchUrl","/contents");
            return "warningPage";
        }

        model.addAttribute("form",findBoard);

        return "board/editForm";
    }

    @Transactional
    @PostMapping("/content/{contentId}/edit")
    public String edit(@Validated @ModelAttribute("form") BoardDto form, BindingResult bindingResult,
                       @PathVariable Long contentId) throws IOException {

        if(bindingResult.hasErrors()){
            return "board/editForm";
        }

        Board editedBoard = boardService.edit(form, contentId);

        if(form.getImageFiles().stream().anyMatch(file -> !file.isEmpty())){
            filesService.updateFile(contentId,editedBoard,form);
        }else{
            filesService.deleteFilesAndEntity(contentId);
        }

        return "redirect:/contents";
    }

    @Transactional
    @GetMapping("/delete/content/{contentId}")
    public String delete(@PathVariable Long contentId,Model model){
        Board findBoard = boardService.findById(contentId);
        if(!namesEq(findBoard.getMember().getUsername())){
            model.addAttribute("message","다른 사용자의 글입니다.");
            model.addAttribute("searchUrl","/contents");
            return "warningPage";
        }

        filesService.deleteFiles(contentId);
        boardService.delete(contentId);


        model.addAttribute("message","글이 삭제되었습니다.");
        model.addAttribute("searchUrl","/contents");
        return "warningPage";
    }


    @GetMapping("/warning")
    public String errorPage(Model model){
        model.addAttribute("message","다른 사용자의 글입니다.");
        model.addAttribute("searchUrl","/contents");
        return "warningPage";
    }

    @GetMapping("/contents")
    public String contentPaging(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "5") int size, Model model,
                                @ModelAttribute("searchCondition") SearchCondition searchCondition){

        PageRequest pageable = PageRequest.of(page,size);
        Page<SearchContentDto> result = boardService.pagingContent(searchCondition,pageable);
        log.info("result.getSize()={}",result.getTotalPages());
        model.addAttribute("contentPage",result);

        return "board/pagingContent";
    }



}
