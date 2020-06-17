package com.lambdaschool.shoppingcart.controllers;

import com.lambdaschool.shoppingcart.models.Cart;
import com.lambdaschool.shoppingcart.models.Product;
import com.lambdaschool.shoppingcart.models.User;
import com.lambdaschool.shoppingcart.repositories.UserRepository;
import com.lambdaschool.shoppingcart.services.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/carts")
public class CartController
{
    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping(value = "/user", produces = {"application/json"})
    public ResponseEntity<?> listAllCartsByAuth()
    {
        List<Cart> myCarts = cartService.findAllByAuth();
        return new ResponseEntity<>(myCarts, HttpStatus.OK);
    }

    @GetMapping(value = "/user/{userid}", produces = {"application/json"})
    public ResponseEntity<?> listAllCarts(@PathVariable long userid)
    {
        List<Cart> myCarts = cartService.findAllByUserId(userid);
        return new ResponseEntity<>(myCarts, HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @GetMapping(value = "/cart/{cartId}",
            produces = {"application/json"})
    public ResponseEntity<?> getCartById(
            @PathVariable
                    Long cartId)
    {
        Cart p = cartService.findCartById(cartId);
        return new ResponseEntity<>(p,
                                    HttpStatus.OK);
    }

    @PostMapping(value = "/create/product/{productid}")
    public ResponseEntity<?> addNewCart(@PathVariable long productid)
    {
        User dataUser = new User();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        dataUser.setUserid(userRepository.findByUsername(authentication.getName()).getUserid());

        Product dataProduct = new Product();
        dataProduct.setProductid(productid);

        cartService.save(dataUser, dataProduct);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping(value = "/update/cart/{cartid}/product/{productid}")
    public ResponseEntity<?> updateCart(@PathVariable long cartid,
                                        @PathVariable long productid)
    {
        Cart dataCart = new Cart();
        dataCart.setCartid(cartid);

        Product dataProduct = new Product();
        dataProduct.setProductid(productid);

        cartService.save(dataCart, dataProduct);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping(value = "/delete/cart/{cartid}/product/{productid}")
    public ResponseEntity<?> deleteFromCart(@PathVariable long cartid,
                                            @PathVariable long productid)
    {
        Cart dataCart = new Cart();
        dataCart.setCartid(cartid);

        Product dataProduct = new Product();
        dataProduct.setProductid(productid);

        cartService.delete(dataCart, dataProduct);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
